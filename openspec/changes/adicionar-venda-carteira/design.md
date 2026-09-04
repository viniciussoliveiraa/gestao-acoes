## Context

Ver `proposal.md` para motivação. Estado atual relevante:

- `TipoLancamento` tem só `COMPRA`; `Lancamento` fixa esse valor no construtor.
- `CarteiraService.listarPosicoes` chama `LancamentoRepository.agregarPosicoesPorUsuario`, uma query JPQL com `GROUP BY l.acao` que retorna `PosicaoAgregada(acao, SUM(quantidade), SUM(quantidade*precoUnitario))`. Essa agregação é comutativa/order-independent — soma tudo de uma vez, sem noção de "preço médio no momento de cada venda".
- O projeto já rejeitou explicitamente persistir uma tabela `Posicao` (ver design.md do MVP original) para evitar dupla escrita/drift; a posição é sempre recalculada a partir dos `Lancamento`s.
- `spring.jpa.open-in-view=false` — qualquer acesso a associações lazy fora da transação do service quebra; o service já precisa carregar tudo que precisar antes de retornar.

## Goals / Non-Goals

**Goals:**
- Suportar lançamento de venda com validação de saldo.
- Calcular preço médio por custo médio ponderado (venda não move o preço médio; só compra move).
- Expor resultado realizado acumulado por ativo.
- Manter o modelo "sem tabela de posição persistida" (cálculo on-the-fly), só que agora processado em Java em vez de SQL puro.

**Non-Goals:**
- FIFO/LIFO ou qualquer outro critério de custo (só custo médio ponderado, que é o único método pedido).
- Apuração de IR (DARF, isenção de R$20mil/mês, day-trade) — fora de escopo, é só o resultado bruto realizado por ativo.
- Edição/exclusão de lançamentos já existentes (fora de escopo desta mudança; um lançamento errado hoje só pode ser corrigido criando lançamentos compensatórios).
- Migrar para uma tabela de posição persistida — mantém a decisão original do MVP.

## Decisions

### Cálculo de posição migra de SQL agregado para processamento em ordem cronológica no service

**Decisão**: `CarteiraService` passa a buscar todos os lançamentos do usuário ordenados por `(acaoId, dataOperacao, criadoEm)` — via uma nova query no repository (`findByUsuarioIdOrderByAcaoIdAscDataOperacaoAscCriadoEmAsc` ou `@Query` equivalente com `JOIN FETCH`, seguindo o mesmo padrão de `findByUsuarioId`) — e processa em memória, agrupando por ativo e acumulando `quantidade`, `precoMedio` e `resultadoRealizado` a cada lançamento na ordem em que ocorreram.

**Por quê**: custo médio ponderado com venda é inerentemente order-dependent (o preço médio "no momento da venda" depende de todas as compras/vendas anteriores daquele ativo). Isso não é expressável em um `SUM()` agregado do banco sem window functions complexas e dependentes de dialeto SQL (o projeto já roda H2 em teste e MySQL em produção via Liquibase — ver `MysqlMigrationSmokeTest`). Processar em Java mantém a lógica portável entre os dois bancos e testável isoladamente sem depender de recursos específicos de SQL.

**Alternativa rejeitada**: window functions (`SUM() OVER`) na query — cálculo de preço médio com reset por venda não é uma simples soma cumulativa, exigiria uma função recursiva ou stored procedure; complexidade maior e menos portável entre H2 (testes) e MySQL (produção) para ganho de desempenho que não é necessário na escala do projeto (poucos lançamentos por usuário).

**Trade-off aceito**: a carga de `GET /carteira/posicoes` passa a trazer *todos* os lançamentos do usuário para memória a cada chamada, em vez de já vir agregado do banco. Aceitável na escala do projeto (uso didático/pessoal, não uma corretora); se o volume crescer, o algoritmo pode futuramente processar por página/streaming sem mudar o contrato da API.

### Preço médio ponderado: fórmula

Para cada ativo, iterando os lançamentos em ordem cronológica, mantendo `quantidade`, `precoMedio` e `resultadoRealizado` (inicialmente zero):

- **Compra** de `q` unidades a `p`: `precoMedio = (quantidade × precoMedio + q × p) / (quantidade + q)`; `quantidade += q`.
- **Venda** de `q` unidades a `p`: `resultadoRealizado += (p − precoMedio) × q`; `quantidade -= q`. `precoMedio` não muda.

Isso corresponde exatamente ao método de custo médio usado pela B3/Receita Federal para apuração de ganho de capital em renda variável.

### Validação de venda acima do saldo

**Decisão**: nova exceção `SaldoInsuficienteException extends ApiException` com `HttpStatus.UNPROCESSABLE_ENTITY` (422), seguindo o padrão já usado no projeto (`ApiException` + subclasses como `AcaoDuplicadaException` com 409). A validação roda em `CarteiraService.registrarLancamento`: antes de persistir uma venda, o service recalcula a posição atual do ativo para o usuário (reaproveitando o mesmo algoritmo de agregação, olhando só o ativo em questão) e rejeita se `quantidade solicitada > quantidade líquida atual`.

**Por quê 422 e não 400**: a requisição é sintaticamente válida (campos corretos), mas semanticamente inválida dado o estado atual da carteira — mesma categoria de "regra de negócio violada" que outras exceções do projeto tratam fora do `400` de bean validation.

**Concorrência**: duas vendas simultâneas do mesmo ativo poderiam ambas passar na validação antes de qualquer uma persistir (race condition clássica de "check-then-act"). Não mitigado nesta mudança — mesmo nível de rigor que o restante do projeto (didático, sem controle de concorrência otimista/pessimista em outros fluxos). Registrado como risco abaixo.

### Enum `TipoLancamento` e coluna no banco

**Decisão**: adicionar `VENDA` ao enum Java. A coluna `tipo` já é `VARCHAR(10)` sem `CHECK` constraint visível no schema atual (mapeada via `@Enumerated(EnumType.STRING)` + `@JdbcTypeCode(SqlTypes.VARCHAR)`) — então, na prática, não é necessária uma migration de schema para o novo valor caber na coluna. Ainda assim, o projeto usa Liquibase para todo histórico de schema; se o changelog existente tiver algum `CHECK` ou comentário documentando os valores aceitos, uma migration de documentação/constraint deve ser adicionada para manter o changelog como fonte da verdade — a tarefa de implementação deve inspecionar os changelogs existentes antes de decidir se uma migration nova é necessária.

### Resultado realizado exposto em `PosicaoResponse`

**Decisão**: adicionar campo `resultadoRealizado` (BigDecimal) ao record `PosicaoResponse`, representando o lucro/prejuízo acumulado de vendas daquele ativo até o momento. Ativos com quantidade líquida zero não aparecem na lista de posições (nada para exibir como posição aberta), mas o resultado realizado de um ativo totalmente vendido não é exposto nesta mudança (não há onde exibi-lo, já que a posição não aparece mais) — fica como extensão futura natural (ex. endpoint de histórico de resultados) se for necessário.

## Risks / Trade-offs

- **[Risco] Condição de corrida em vendas simultâneas do mesmo ativo pode permitir posição negativa** → aceito como limitação didática nesta mudança, documentado aqui e no spec como algo a considerar caso o projeto evolua para uso concorrente real.
- **[Trade-off] `GET /carteira/posicoes` carrega todos os lançamentos do usuário em memória a cada chamada** → aceitável na escala do projeto; ver decisão acima.
- **[Risco] Resultado realizado de ativos totalmente vendidos "desaparece" da resposta da API** → aceito nesta mudança; não é perdido (pode ser recalculado a qualquer momento a partir do histórico de lançamentos), só não é exibido ainda.

## Migration Plan

1. Adicionar `VENDA` ao enum `TipoLancamento` e ajustar `Lancamento` para aceitar `tipo` no construtor/factory de criação.
2. Inspecionar changelogs Liquibase existentes da tabela `lancamento`; adicionar migration nova apenas se houver constraint que precise ser atualizada para aceitar `VENDA`.
3. Implementar o novo algoritmo de cálculo de posição no `CarteiraService` e a nova query ordenada no `LancamentoRepository`, mantendo `agregarPosicoesPorUsuario`/`PosicaoAgregada` só se ainda forem usados por algo — caso contrário remover para não deixar código morto.
4. Adicionar `SaldoInsuficienteException` e a validação em `registrarLancamento`.
5. Atualizar `LancamentoRequest`/`LancamentoResponse`/`PosicaoResponse` e mappers.
6. Atualizar frontend Angular (formulário e tela de posições).
7. Sem rollback especial necessário: é uma adição aditiva ao schema (nenhuma coluna/tabela removida) — reverter é só reverter o deploy do backend/frontend.
