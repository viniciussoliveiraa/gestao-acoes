## Purpose

Permite que um usuário autenticado registre compras/aportes de ações (lançamentos) em corretoras cadastradas, e consulte a posição consolidada da sua carteira por ativo, calculada a partir desses lançamentos.

## ADDED Requirements

### Requirement: Registro de lançamento de compra
O sistema SHALL aceitar `POST /carteira/lancamentos`, autenticado, recebendo `acaoId` (ou `ticker`), `corretoraId`, `quantidade`, `precoUnitario` e `dataOperacao`, associando o lançamento ao usuário autenticado. O sistema SHALL validar que a ação e a corretora informadas existem antes de persistir o lançamento.

#### Scenario: Lançamento bem-sucedido
- **GIVEN** uma ação `PETR4` e uma corretora já cadastradas no sistema
- **WHEN** o usuário autenticado envia `POST /carteira/lancamentos` com `acaoId`, `corretoraId`, `quantidade: 100`, `precoUnitario: 32.50`, `dataOperacao: "2026-08-10"`
- **THEN** o sistema responde `201` com o lançamento criado, associado ao usuário autenticado

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
O sistema SHALL aceitar `GET /carteira/posicoes`, autenticado, retornando uma posição consolidada por ativo com, no mínimo: ticker/nome da ação, quantidade total, preço médio ponderado, valor investido (quantidade × preço médio), valor atual (quantidade × `cotacaoAtual` da ação) e variação percentual entre valor investido e valor atual — calculados a partir de todos os lançamentos do usuário autenticado para aquele ativo.

#### Scenario: Posição com um único lançamento
- **GIVEN** um único lançamento do usuário autenticado de `100` ações de `PETR4` a `R$ 32,50`
- **WHEN** o usuário autenticado chama `GET /carteira/posicoes`
- **THEN** o sistema responde `200` com uma posição de `PETR4`, quantidade `100`, preço médio `32,50`, valor investido `3.250,00`

#### Scenario: Posição com múltiplos lançamentos do mesmo ativo
- **GIVEN** dois lançamentos do usuário autenticado do mesmo ativo, com quantidades e preços diferentes
- **WHEN** o usuário autenticado chama `GET /carteira/posicoes`
- **THEN** o sistema responde `200` com uma única posição consolidada daquele ativo, com quantidade somada e preço médio ponderado pelas quantidades de cada lançamento

#### Scenario: Carteira vazia
- **GIVEN** um usuário autenticado sem nenhum lançamento registrado
- **WHEN** ele chama `GET /carteira/posicoes`
- **THEN** o sistema responde `200` com uma lista vazia

### Requirement: Listagem de lançamentos do usuário
O sistema SHALL aceitar `GET /carteira/lancamentos`, autenticado, retornando os lançamentos do usuário autenticado, paginados.

#### Scenario: Listagem paginada dos próprios lançamentos
- **WHEN** o usuário autenticado chama `GET /carteira/lancamentos?page=0&size=20`
- **THEN** o sistema responde `200` com uma página contendo apenas lançamentos daquele usuário

### Requirement: Isolamento de dados entre usuários
O sistema NÃO SHALL expor lançamentos ou posições de um usuário a outro usuário autenticado, independentemente do endpoint chamado.

#### Scenario: Usuário não vê lançamentos de outro usuário
- **GIVEN** os usuários A e B, cada um com lançamentos próprios
- **WHEN** o usuário B chama `GET /carteira/lancamentos` ou `GET /carteira/posicoes`
- **THEN** o sistema retorna apenas os dados pertencentes ao usuário B, nunca ao usuário A
