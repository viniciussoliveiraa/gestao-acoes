## Purpose

Isola a consulta de dados cadastrais de empresas por CNPJ (via BrasilAPI) atrás de uma porta interna, convertendo a resposta externa em um modelo interno e nunca expondo o DTO do fornecedor às camadas de domínio.

## ADDED Requirements

### Requirement: Porta interna de consulta de dados empresariais
O sistema SHALL definir uma porta interna (interface) para consulta de dados cadastrais por CNPJ, consumida pelo serviço de corretoras, e um adaptador concreto para a BrasilAPI que implementa essa porta. Controllers, entidades JPA e regras de domínio NÃO SHALL depender diretamente do DTO de resposta da BrasilAPI.

#### Scenario: Consulta bem-sucedida
- **WHEN** o serviço de corretoras solicita os dados de um CNPJ válido e existente à porta de dados empresariais
- **THEN** o adaptador consulta a BrasilAPI com o CNPJ normalizado (14 dígitos), mapeia apenas os campos necessários (`razaoSocial`, `nomeFantasia`, `situacaoCadastral`, endereço se disponível) e retorna o modelo interno

### Requirement: Tratamento de CNPJ inexistente no provedor
O adaptador SHALL diferenciar CNPJ inexistente (resposta `404` do provedor) de erro de infraestrutura (timeout, `5xx`, resposta malformada), retornando um resultado que a camada de serviço traduz em `404`/`422` para inexistência e `502`/`503` para falha de infraestrutura.

#### Scenario: BrasilAPI retorna 404 para o CNPJ
- **WHEN** a BrasilAPI responde `404` para o CNPJ consultado
- **THEN** o adaptador sinaliza "CNPJ não encontrado" e não lança uma exceção genérica de infraestrutura

### Requirement: Resiliência da integração
O adaptador SHALL aplicar timeout de conexão e leitura configuráveis e SHALL tratar respostas `429` sem repetição indefinida. Retentativas, se existirem, SHALL se limitar a falhas transitórias, com backoff e limite baixo de tentativas.

#### Scenario: Timeout do provedor
- **WHEN** a BrasilAPI não responde dentro do timeout configurado
- **THEN** o adaptador retorna um erro de indisponibilidade que a camada de serviço traduz em `502` ou `503`, sem persistir dados parciais

#### Scenario: Resposta malformada
- **WHEN** a BrasilAPI retorna um corpo que não pode ser desserializado no DTO esperado
- **THEN** o adaptador trata como falha de integração e não propaga o corpo bruto da resposta ao cliente da API

### Requirement: Segredos e configuração
A URL-base da BrasilAPI SHALL ser configurável via variável de ambiente (`BRASIL_API_BASE_URL`) e NÃO SHALL ser fixada no código-fonte.

#### Scenario: Configuração ausente usa valor padrão documentado
- **WHEN** a variável `BRASIL_API_BASE_URL` não é definida no ambiente
- **THEN** o sistema usa o valor padrão documentado no `README.md` e no `application.properties` de exemplo