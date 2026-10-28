## Purpose

Isola a obtenção de cotações de ações brasileiras (brapi.dev) e americanas (Twelve Data ou Alpha Vantage) atrás de portas internas, selecionando o adaptador correto por mercado via Strategy/Factory.

## ADDED Requirements

### Requirement: Porta interna de cotação e seleção por mercado
O sistema SHALL definir uma porta interna de cotação de ações, com uma estratégia de seleção de adaptador por mercado: brapi.dev para `BRASIL`; o provedor configurado em `app.market-data.us-provider` (`twelve-data` ou `alpha-vantage`) para `ESTADOS_UNIDOS`. Controllers, entidades JPA e regras de domínio NÃO SHALL depender diretamente dos DTOs de resposta de qualquer provedor de cotação.

#### Scenario: Seleção do adaptador brasileiro
- **WHEN** o serviço de ações solicita cotação para `mercado: "BRASIL"`
- **THEN** a estratégia seleciona o adaptador brapi.dev

#### Scenario: Seleção do adaptador americano por configuração
- **GIVEN** `app.market-data.us-provider=twelve-data`
- **WHEN** o serviço de ações solicita cotação para `mercado: "ESTADOS_UNIDOS"`
- **THEN** a estratégia seleciona o adaptador Twelve Data, e não Alpha Vantage

### Requirement: Contrato de cotação brasileira (brapi.dev)
O adaptador brapi.dev SHALL consultar o contrato de cotação vigente para o ticker informado, confirmado em `design.md`, retornando símbolo, preço mais recente e timestamp. Quando o ticker não exigir token e não estiver entre os símbolos de demonstração disponíveis sem autenticação, o adaptador SHALL tratar a ausência de `BRAPI_TOKEN` como configuração inválida para esse ticker, retornando erro explícito em vez de dado incompleto.

#### Scenario: Ticker de demonstração sem token
- **WHEN** o ticker consultado está disponível sem autenticação e `BRAPI_TOKEN` não está configurado
- **THEN** o adaptador retorna a cotação normalmente

#### Scenario: Ticker que exige token ausente
- **WHEN** o ticker consultado exige `BRAPI_TOKEN` e a variável não está configurada
- **THEN** o adaptador retorna erro de configuração, e o serviço responde `502` ou `503` conforme `design.md`, sem persistir a ação

### Requirement: Contrato de cotação americana (Twelve Data / Alpha Vantage)
O adaptador Twelve Data SHALL usar o endpoint de cotação/preço confirmado em `design.md` (`/quote` ou `/price`), retornando símbolo, moeda, preço mais recente e timestamp. O adaptador Alpha Vantage SHALL ser implementado como estratégia alternativa selecionável por configuração, usando `GLOBAL_QUOTE` para cotação pontual, e NÃO SHALL ser chamado automaticamente em paralelo ao Twelve Data no MVP.

#### Scenario: Provedor americano configurado responde com sucesso
- **GIVEN** `app.market-data.us-provider=alpha-vantage`
- **WHEN** o serviço de ações solicita cotação para um ticker americano válido
- **THEN** apenas o adaptador Alpha Vantage é chamado, e o Twelve Data não é consultado

### Requirement: Validação de ticker inexistente
Cada adaptador de cotação SHALL diferenciar ticker inexistente (confirmado pelo provedor) de erro de infraestrutura (timeout, `5xx`, resposta malformada), permitindo que a camada de serviço responda `404`/`422` para ticker inexistente e `502`/`503` para falha de infraestrutura.

#### Scenario: Ticker não reconhecido pelo provedor
- **WHEN** o provedor do mercado informado não reconhece o ticker consultado
- **THEN** o adaptador sinaliza "ticker não encontrado", distinto de uma falha de infraestrutura

### Requirement: Resiliência e limites de requisição
Cada adaptador SHALL aplicar timeout de conexão e leitura configuráveis e SHALL tratar respostas `429` (limite de requisições excedido) retornando erro explícito ao serviço, sem retentativa em loop. Retentativas, quando existirem, SHALL se limitar a falhas transitórias, com backoff e limite baixo de tentativas.

#### Scenario: Limite de requisições excedido
- **WHEN** o provedor de cotação responde `429`
- **THEN** o adaptador não repete a chamada indefinidamente e sinaliza a condição de limite excedido para a camada de serviço

### Requirement: Segredos e configuração
As URLs-base e chaves de API de todos os provedores de cotação (`BRAPI_BASE_URL`, `BRAPI_TOKEN`, `TWELVE_DATA_BASE_URL`, `TWELVE_DATA_API_KEY`, `ALPHA_VANTAGE_BASE_URL`, `ALPHA_VANTAGE_API_KEY`) SHALL ser configuráveis por variável de ambiente e NÃO SHALL ser fixadas no código-fonte, impressas em log ou versionadas.

#### Scenario: Chave de API ausente para o provedor configurado
- **WHEN** o provedor americano configurado exige uma chave de API que não está definida no ambiente
- **THEN** o sistema falha de forma explícita na inicialização ou na primeira chamada, sem expor o valor esperado da chave em log ou resposta

### Requirement: Suíte de testes sem chamadas reais
Os testes automatizados de todos os adaptadores de cotação SHALL usar respostas simuladas (MockWebServer/WireMock) e NÃO SHALL consumir cotas reais dos provedores externos durante a execução padrão da suíte.

#### Scenario: Execução da suíte padrão
- **WHEN** a suíte de testes automatizados é executada localmente ou em CI
- **THEN** nenhuma chamada de rede real é feita a brapi.dev, Twelve Data ou Alpha Vantage