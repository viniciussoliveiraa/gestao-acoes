## Purpose

Permite cadastrar ações brasileiras e americanas com ticker validado e cotação obtida de um provedor externo, consultá-las por listagem paginada, ID ou ticker (globalmente único), e atualizar a cotação sob demanda.

## ADDED Requirements

### Requirement: Cadastro de ação com ticker e mercado explícitos
O sistema SHALL aceitar `POST /acoes` recebendo `ticker` e `mercado` (`BRASIL` ou `ESTADOS_UNIDOS`), normalizando o ticker (maiúsculas, sem espaços) e o mercado antes de qualquer comparação ou chamada externa.

#### Scenario: Ticker enviado em minúsculas com espaços
- **WHEN** o cliente envia `POST /acoes` com `ticker: " petr4 "` e `mercado: "BRASIL"`
- **THEN** o sistema normaliza o ticker para `PETR4` antes de validá-lo no provedor e de persistir

### Requirement: Roteamento por mercado ao provedor de cotação correto
O sistema SHALL selecionar o adaptador de cotação apropriado para o mercado informado: brapi.dev para `BRASIL`; Twelve Data ou Alpha Vantage (conforme `app.market-data.us-provider`) para `ESTADOS_UNIDOS`. O sistema NÃO SHALL consultar um provedor de mercado incompatível com o ticker informado.

#### Scenario: Mercado incompatível com o ticker
- **WHEN** o cliente cadastra `ticker: "AAPL"` com `mercado: "BRASIL"`
- **THEN** o provedor brasileiro não confirma o ticker e o sistema responde `422` ou `404` (conforme `design.md`), sem persistir a ação

### Requirement: Validação de ticker existente antes do cadastro
O sistema SHALL confirmar, por meio da capacidade `integracao-cotacoes`, que o ticker existe no provedor do mercado informado antes de persistir a ação. O sistema SHALL obter e persistir `ticker`, `nomeEmpresa` (quando disponível), `mercado`, `moeda`, `cotacaoAtual` e `dataHoraCotacao` a partir da resposta do provedor.

#### Scenario: Ticker inexistente
- **WHEN** o cliente cadastra um ticker que o provedor do mercado informado não reconhece
- **THEN** o sistema responde `404` ou `422` (conforme `design.md`) informando que o ticker não foi encontrado, e nenhuma ação é persistida

### Requirement: Unicidade global de ticker
O sistema SHALL impedir o cadastro de duas ações com o mesmo `ticker` normalizado, independentemente do mercado (RN07 do enunciado original: "Não será permitido cadastrar duas ações com o mesmo ticker"). Essa restrição SHALL ser garantida tanto na aplicação quanto por constraint única no banco de dados sobre a coluna `ticker`.

#### Scenario: Ticker duplicado no mesmo mercado
- **GIVEN** uma ação `PETR4` já cadastrada no mercado `BRASIL`
- **WHEN** o cliente envia `POST /acoes` com `ticker: "PETR4"` e `mercado: "BRASIL"`
- **THEN** o sistema responde `409` com Problem Details indicando duplicidade, e nenhuma nova ação é criada

#### Scenario: Mesmo símbolo em mercados diferentes também é rejeitado
- **GIVEN** uma ação `IBM` já cadastrada no mercado `ESTADOS_UNIDOS`
- **WHEN** o cliente cadastra `ticker: "IBM"` e `mercado: "BRASIL"`
- **THEN** o sistema responde `409`, pois a unicidade de ticker é global, não por mercado

### Requirement: Precisão monetária e timezone da cotação
O sistema SHALL armazenar `cotacaoAtual` usando `BigDecimal`, com escala e arredondamento explicitamente definidos em `design.md`, e NÃO SHALL usar `double` ou `float` para valores monetários. O sistema SHALL preservar o offset/timezone de `dataHoraCotacao` conforme a política documentada em `design.md`.

#### Scenario: Cotação retornada pelo provedor é persistida com precisão definida
- **WHEN** o provedor de cotação retorna um preço com mais casas decimais do que a escala definida para a moeda
- **THEN** o sistema aplica o arredondamento documentado em `design.md` antes de persistir `cotacaoAtual`

### Requirement: Persistência atômica do cadastro de ação
O sistema NÃO SHALL persistir uma ação parcialmente cadastrada. Se a consulta ao provedor de cotação falhar após a validação do ticker, nenhuma linha SHALL ser gravada.

#### Scenario: Falha do provedor após confirmação do ticker
- **WHEN** o ticker é confirmado, mas a chamada subsequente para obter a cotação falha por timeout
- **THEN** o sistema responde `502` ou `503` e nenhuma ação é persistida

### Requirement: Consulta de ações
O sistema SHALL permitir listar ações com paginação, buscar uma ação por ID e buscar uma ação por ticker (sem necessidade de informar o mercado, já que o ticker é globalmente único).

#### Scenario: Listagem paginada
- **WHEN** o cliente chama `GET /acoes?page=0&size=20`
- **THEN** o sistema responde `200` com uma página de ações e metadados de paginação

#### Scenario: Busca por ticker existente
- **WHEN** o cliente chama `GET /acoes/ticker/PETR4`
- **THEN** o sistema responde `200` com a ação correspondente, ou `404` caso não exista

### Requirement: Atualização sob demanda da cotação
O sistema SHALL permitir atualizar a cotação de uma ação já cadastrada via `PUT /acoes/{id}/atualizar-cotacao`, consultando novamente o provedor apropriado ao mercado da ação. Se a consulta externa falhar, o sistema SHALL manter a última cotação válida armazenada e retornar um erro explícito ao cliente, sem alterar o registro.

#### Scenario: Atualização bem-sucedida
- **WHEN** o cliente chama `PUT /acoes/{id}/atualizar-cotacao` para uma ação existente e o provedor responde com sucesso
- **THEN** o sistema atualiza `cotacaoAtual` e `dataHoraCotacao` e responde `200` com a ação atualizada

#### Scenario: Falha do provedor durante atualização
- **WHEN** o cliente chama `PUT /acoes/{id}/atualizar-cotacao` e o provedor de cotação está indisponível
- **THEN** o sistema responde `502` ou `503`, mantém a última cotação válida sem alterações e não persiste um estado intermediário