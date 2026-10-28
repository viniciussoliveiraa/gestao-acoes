## 1. Modelo e persistência

- [x] 1.1 Adicionar `VENDA` a `TipoLancamento` (`src/main/java/br/com/gestaoacoes/model/TipoLancamento.java`) e atualizar o comentário da classe.
- [x] 1.2 Alterar `Lancamento` para aceitar `TipoLancamento tipo` no construtor em vez de fixar `COMPRA`.
- [x] 1.3 Inspecionar `src/main/resources/db/changelog/changes/004-create-lancamento.xml` (e changelogs seguintes) em busca de `CHECK`/constraint que limite os valores de `tipo`; se nenhum existir, registrar no PR/commit que nenhuma migration nova é necessária (coluna já é `VARCHAR(10)`). Se existir, adicionar changeset novo (nunca editar um changeset já aplicado) liberando `VENDA`. — Nenhum `CHECK`/constraint encontrado; nenhuma migration nova foi necessária.

## 2. Regra de negócio: validação de saldo

- [x] 2.1 Criar `SaldoInsuficienteException extends ApiException` com `HttpStatus.UNPROCESSABLE_ENTITY` (422), seguindo o padrão de `AcaoDuplicadaException`.
- [x] 2.2 Em `CarteiraService.registrarLancamento`, quando `request.tipo() == VENDA`, calcular a quantidade líquida atual do usuário para o ativo e lançar `SaldoInsuficienteException` se `quantidade > saldo disponível`. — Implementado com uma query dedicada (`findByUsuarioIdAndAcaoId`) mais simples que reaproveitar o algoritmo completo de custo médio, já que a validação de saldo só precisa da soma líquida (compra − venda), não do preço médio.
- [x] 2.3 Testar em `CarteiraServiceTest`: venda dentro do saldo passa; venda acima do saldo lança `SaldoInsuficienteException` e não persiste; venda de ativo sem nenhum lançamento anterior lança a mesma exceção.

## 3. Cálculo de posição por custo médio ponderado

- [x] 3.1 Adicionar a `LancamentoRepository` uma query que retorne todos os lançamentos de um usuário com `acao`/`corretora` já carregados (`JOIN FETCH`), ordenados por ativo, `dataOperacao` e `criadoEm` — seguindo o mesmo padrão de `findByUsuarioId`.
- [x] 3.2 Reescrever `CarteiraService.listarPosicoes`/`calcularPosicao` para processar os lançamentos em ordem cronológica por ativo, mantendo `quantidade`, `precoMedio` e `resultadoRealizado` conforme a fórmula do `design.md` ("Preço médio ponderado: fórmula"). — Agrupamento por ativo feito pela própria entidade `Acao` (identidade de objeto, garantida pelo Hibernate dentro da mesma sessão/JOIN FETCH), não por `acao.getId()`, para não depender de IDs (relevante em teste unitário com entidades transitórias).
- [x] 3.3 Remover `agregarPosicoesPorUsuario`/`PosicaoAgregada` se não sobrarem outros usos, para não deixar código morto. — Removidos (arquivo `PosicaoAgregada.java` deletado).
- [x] 3.4 Adicionar `resultadoRealizado` a `PosicaoResponse` e ao mapper correspondente.
- [x] 3.5 Garantir que ativos com quantidade líquida zero não apareçam em `GET /carteira/posicoes`.
- [x] 3.6 Testar em `CarteiraServiceTest`: compra única; múltiplas compras (preço médio ponderado); venda parcial (preço médio inalterado, resultado realizado correto); venda total (ativo some da lista); compra após venda parcial (preço médio recalculado só sobre o saldo remanescente); múltiplos ativos e múltiplos usuários não se misturam.

## 4. API: request/response

- [x] 4.1 Adicionar `tipo` (opcional, default `COMPRA`) a `LancamentoRequest`, com validação de enum.
- [x] 4.2 Adicionar `tipo` a `LancamentoResponse` e atualizar `LancamentoMapper`.
- [x] 4.3 Atualizar `CarteiraControllerTest` e `CarteiraFluxoIntegrationTest` cobrindo: registrar venda, venda acima do saldo (422), listagem de lançamentos mostrando `tipo`, posições refletindo custo médio ponderado.
- [x] 4.4 Atualizar a documentação OpenAPI (`OpenApiConfig`/anotações nos DTOs, se existirem) para o novo campo `tipo` e o novo status 422. — Não há anotações OpenAPI manuais no projeto (springdoc documenta os controllers/DTOs automaticamente); nada a alterar além do próprio código-fonte.

## 5. Frontend Angular

- [x] 5.1 Em `frontend/src/app/features/lancamentos`, adicionar seleção de tipo (compra/venda) ao formulário de novo lançamento.
- [x] 5.2 Tratar o erro 422 de saldo insuficiente no formulário, exibindo mensagem amigável ao usuário. — Já coberto pelo tratamento genérico existente (`mensagemDeErro`, que extrai `detail` de qualquer Problem Details); nenhuma mudança de código necessária.
- [x] 5.3 Em `frontend/src/app/features/rebalanceamento` (e qualquer outra tela que liste posições), exibir/considerar o novo campo `resultadoRealizado` quando fizer sentido para a tela.
- [x] 5.4 Ajustar testes de frontend (specs Angular) para os componentes alterados. — Não há specs para os componentes de feature (`lancamentos`/`rebalanceamento`) no projeto, só o boilerplate padrão `app.spec.ts`; nada a ajustar. `ng build` e `tsc --noEmit` passam sem erros.

## 6. Documentação

- [x] 6.1 Atualizar `README.md`/roteiro de demonstração removendo a limitação "somente compra" onde for mencionada.
- [x] 6.2 Conferir que `openspec validate --strict` passa para a mudança `adicionar-venda-carteira` antes de arquivar.
