## MODIFIED Requirements

### Requirement: Registro de lançamento de compra
O sistema SHALL aceitar `POST /carteira/lancamentos`, autenticado, recebendo `acaoId` (ou `ticker`), `corretoraId`, `tipo` (`COMPRA` ou `VENDA`), `quantidade`, `precoUnitario` e `dataOperacao`, associando o lançamento ao usuário autenticado. O sistema SHALL validar que a ação e a corretora informadas existem antes de persistir o lançamento. Quando `tipo` for omitido, o sistema SHALL assumir `COMPRA` por padrão (compatibilidade com o comportamento anterior). Quando `tipo` for `VENDA`, o sistema SHALL validar que a quantidade vendida não excede a quantidade líquida que o usuário possui daquele ativo (considerando todos os lançamentos anteriores do usuário para o ativo); o sistema NÃO SHALL permitir que a posição resultante de um ativo fique negativa.

#### Scenario: Lançamento de compra bem-sucedido
- **GIVEN** uma ação `PETR4` e uma corretora já cadastradas no sistema
- **WHEN** o usuário autenticado envia `POST /carteira/lancamentos` com `acaoId`, `corretoraId`, `tipo: "COMPRA"`, `quantidade: 100`, `precoUnitario: 32.50`, `dataOperacao: "2026-08-10"`
- **THEN** o sistema responde `201` com o lançamento criado, associado ao usuário autenticado

#### Scenario: Lançamento de venda bem-sucedido
- **GIVEN** o usuário autenticado possui `100` ações de `PETR4` em sua posição
- **WHEN** ele envia `POST /carteira/lancamentos` com `acaoId` de `PETR4`, `tipo: "VENDA"`, `quantidade: 40`, `precoUnitario: 35.00`, `dataOperacao: "2026-08-20"`
- **THEN** o sistema responde `201` com o lançamento de venda criado, associado ao usuário autenticado

#### Scenario: Venda acima da posição disponível
- **GIVEN** o usuário autenticado possui `100` ações de `PETR4` em sua posição
- **WHEN** ele envia `POST /carteira/lancamentos` com `tipo: "VENDA"` e `quantidade: 150` para `PETR4`
- **THEN** o sistema responde `422` com Problem Details informando saldo insuficiente, e nenhum lançamento é persistido

#### Scenario: Venda de ativo sem posição
- **GIVEN** o usuário autenticado não possui nenhum lançamento de `VALE3`
- **WHEN** ele envia `POST /carteira/lancamentos` com `tipo: "VENDA"` para `VALE3`
- **THEN** o sistema responde `422` com Problem Details informando saldo insuficiente, e nenhum lançamento é persistido

#### Scenario: Ação inexistente
- **WHEN** o usuário autenticado envia `POST /carteira/lancamentos` referenciando um `acaoId` que não existe
- **THEN** o sistema responde `404` com Problem Details, e nenhum lançamento é persistido

#### Scenario: Corretora inexistente
- **WHEN** o usuário autenticado envia `POST /carteira/lancamentos` referenciando um `corretoraId` que não existe
- **THEN** o sistema responde `404` com Problem Details, e nenhum lançamento é persistido

#### Scenario: Quantidade ou preço inválidos
- **WHEN** o usuário autenticado envia `POST /carteira/lancamentos` com `quantidade` ou `precoUnitario` menor ou igual a zero
- **THEN** o sistema responde `400` com Problem Details, e nenhum lançamento é persistido

### Requirement: Consulta de posições consolidadas
O sistema SHALL aceitar `GET /carteira/posicoes`, autenticado, retornando uma posição consolidada por ativo com, no mínimo: ticker/nome da ação, quantidade líquida (compras menos vendas), preço médio ponderado, valor investido (quantidade líquida × preço médio), valor atual (quantidade líquida × `cotacaoAtual` da ação), variação percentual entre valor investido e valor atual, e resultado realizado acumulado com vendas daquele ativo.

O preço médio SHALL ser calculado pelo método de custo médio ponderado: cada lançamento de `COMPRA` recalcula o preço médio como a média ponderada entre a posição anterior e a nova compra; cada lançamento de `VENDA` reduz a quantidade líquida SEM alterar o preço médio corrente, e soma ao resultado realizado do ativo o valor `(precoUnitario da venda − preço médio no momento da venda) × quantidade vendida`. Os lançamentos SHALL ser processados em ordem cronológica de `dataOperacao` (com o momento de criação do lançamento como critério de desempate) para que o cálculo seja determinístico. Ativos cuja quantidade líquida chegue a zero NÃO SHALL aparecer na lista de posições, mas o resultado realizado desse ativo permanece contabilizado historicamente conforme a Requirement de resultado realizado.

#### Scenario: Posição com um único lançamento
- **GIVEN** um único lançamento do usuário autenticado de `100` ações de `PETR4` a `R$ 32,50`
- **WHEN** o usuário autenticado chama `GET /carteira/posicoes`
- **THEN** o sistema responde `200` com uma posição de `PETR4`, quantidade `100`, preço médio `32,50`, valor investido `3.250,00`

#### Scenario: Posição com múltiplos lançamentos de compra do mesmo ativo
- **GIVEN** dois lançamentos de compra do usuário autenticado do mesmo ativo, com quantidades e preços diferentes
- **WHEN** o usuário autenticado chama `GET /carteira/posicoes`
- **THEN** o sistema responde `200` com uma única posição consolidada daquele ativo, com quantidade somada e preço médio ponderado pelas quantidades de cada compra

#### Scenario: Venda parcial mantém o preço médio
- **GIVEN** o usuário autenticado comprou `100` ações de `PETR4` a `R$ 32,50` e depois vendeu `40` ações a `R$ 35,00`
- **WHEN** ele chama `GET /carteira/posicoes`
- **THEN** o sistema responde `200` com uma posição de `PETR4` com quantidade `60`, preço médio ainda `32,50`, e resultado realizado de `R$ 100,00` (`(35,00 − 32,50) × 40`)

#### Scenario: Venda total zera a posição
- **GIVEN** o usuário autenticado comprou `100` ações de `PETR4` e depois vendeu as `100` ações
- **WHEN** ele chama `GET /carteira/posicoes`
- **THEN** o sistema responde `200` sem nenhuma posição de `PETR4` na lista

#### Scenario: Compra após venda parcial recalcula o preço médio sobre a quantidade remanescente
- **GIVEN** o usuário autenticado comprou `100` ações de `PETR4` a `R$ 32,50`, vendeu `40` a `R$ 35,00` e depois comprou mais `20` a `R$ 40,00`
- **WHEN** ele chama `GET /carteira/posicoes`
- **THEN** o sistema responde `200` com uma posição de `PETR4` com quantidade `80` e preço médio recalculado a partir dos `60` remanescentes a `32,50` e das `20` novas a `40,00`

#### Scenario: Carteira vazia
- **GIVEN** um usuário autenticado sem nenhum lançamento registrado
- **WHEN** ele chama `GET /carteira/posicoes`
- **THEN** o sistema responde `200` com uma lista vazia
