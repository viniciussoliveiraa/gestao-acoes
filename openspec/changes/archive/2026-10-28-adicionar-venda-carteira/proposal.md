## Why

O MVP da gestão de carteira (`adicionar-carteira-auth-frontend-angular`) cobriu conscientemente apenas lançamentos de compra/aporte, deixando venda como diferencial futuro (ver `openspec/changes/adicionar-carteira-auth-frontend-angular/design.md`, "Escopo MVP" e risco aceito na seção de riscos). Sem venda, a carteira nunca reflete uma redução de posição real — um usuário que vendeu uma ação continua vendo a posição cheia, o que torna o sistema inutilizável para acompanhar uma carteira de verdade. Esta mudança implementa o lançamento de venda e corrige o cálculo de preço médio para considerar corretamente compras e vendas juntas.

## What Changes

- `TipoLancamento` ganha o valor `VENDA` (o campo já existia no modelo, fixo em `COMPRA`, preparado para esta extensão).
- `POST /carteira/lancamentos` passa a aceitar `tipo` (`COMPRA` ou `VENDA`) no corpo da requisição.
- Uma venda é validada contra a posição atual do usuário naquele ativo: não é permitido vender quantidade maior do que a quantidade líquida disponível (nunca gerar posição negativa).
- O cálculo de posição (`GET /carteira/posicoes`) passa a usar o método de **custo médio ponderado** (padrão adotado pela B3/Receita Federal): uma compra recalcula o preço médio ponderando pela quantidade comprada; uma venda reduz a quantidade mas **não altera** o preço médio, e realiza um resultado (lucro ou prejuízo) igual a `(precoVenda − precoMedioNoMomento) × quantidadeVendida`.
- **BREAKING (interno)**: o cálculo de posição deixa de ser um `SUM()` agregado no banco (`PosicaoAgregada`/`agregarPosicoesPorUsuario`) — que é independente de ordem e não suporta custo médio ponderado — e passa a processar os lançamentos de cada ativo em ordem cronológica (`dataOperacao`, com `criadoEm` como desempate) no service. Não há mudança de contrato para consumidores externos da API além do novo campo `tipo`.
- `GET /carteira/posicoes` passa a expor também o resultado realizado acumulado (lucro/prejuízo já realizado em vendas) por ativo.
- Frontend Angular: formulário de novo lançamento ganha seleção de tipo (compra/venda) e a tela de posições reflete quantidade líquida, preço médio e resultado realizado.
- Nova migration Liquibase para o valor `VENDA` do enum, caso o dialeto usado exija alteração de schema (a coluna já é `VARCHAR`, então normalmente não bloqueia — a migration cobre apenas o que for necessário, ex. constraint/check existente).

## Capabilities

### New Capabilities
(nenhuma — a funcionalidade se encaixa na capacidade já existente de gestão de carteira)

### Modified Capabilities
- `gestao-carteira`: registro de lançamento passa a aceitar `tipo` (compra/venda) com validação de saldo; cálculo de posição passa a usar custo médio ponderado considerando vendas e a expor resultado realizado.

## Impact

- **Backend**: `TipoLancamento`, `Lancamento`, `LancamentoRequest`/`LancamentoResponse`, `CarteiraService` (novo algoritmo de cálculo de posição), `LancamentoRepository` (troca ou complemento da query agregada por uma consulta ordenada), `PosicaoResponse` (novo campo de resultado realizado), nova exceção de negócio para venda acima do saldo, migration Liquibase.
- **Frontend**: formulário de lançamento (Angular), tela de posições/rebalanceamento.
- **Testes**: `CarteiraServiceTest`, `CarteiraControllerTest`, `CarteiraFluxoIntegrationTest`, `LancamentoRepositoryTest`, e specs equivalentes no frontend.
- **Documentação**: `openspec/specs/gestao-carteira` (delta desta mudança), README/roteiro de demonstração se mencionarem o escopo "somente compra".
