## Purpose

Isola a consulta e validação de CEP (via ViaCEP) atrás de uma porta interna, distinguindo CEP com formato inválido de CEP válido mas inexistente, e convertendo a resposta externa em um modelo de endereço interno.

## ADDED Requirements

### Requirement: Porta interna de consulta de endereço por CEP
O sistema SHALL definir uma porta interna para consulta de endereço por CEP, consumida pelo serviço de corretoras, e um adaptador concreto para a ViaCEP que implementa essa porta usando o contrato JSON por CEP de 8 dígitos.

#### Scenario: Consulta bem-sucedida
- **WHEN** o serviço de corretoras solicita o endereço de um CEP de 8 dígitos válido e existente
- **THEN** o adaptador consulta a ViaCEP e retorna `logradouro`, `bairro`, `cidade` e `uf` no modelo interno

### Requirement: Distinção entre CEP malformado e CEP inexistente
O adaptador SHALL validar localmente que o CEP possui 8 dígitos antes de consultar a ViaCEP, retornando erro de formato sem chamada externa quando inválido. Quando o CEP tiver formato válido mas a ViaCEP retornar `erro: true`, o adaptador SHALL sinalizar "CEP inexistente", distinto do erro de formato.

#### Scenario: CEP com menos de 8 dígitos
- **WHEN** o CEP informado não possui exatamente 8 dígitos numéricos
- **THEN** o adaptador não realiza chamada à ViaCEP e sinaliza erro de formato

#### Scenario: CEP com 8 dígitos mas inexistente
- **WHEN** a ViaCEP responde com `erro: true` para um CEP de 8 dígitos
- **THEN** o adaptador sinaliza "CEP inexistente", distinto de um erro de formato ou de infraestrutura

### Requirement: Resiliência da integração
O adaptador SHALL aplicar timeout de conexão e leitura configuráveis e SHALL tratar indisponibilidade da ViaCEP como falha de infraestrutura, sem repetição indefinida.

#### Scenario: ViaCEP indisponível
- **WHEN** a ViaCEP não responde dentro do timeout configurado
- **THEN** o adaptador retorna um erro de indisponibilidade que a camada de serviço traduz em `502` ou `503`

### Requirement: Segredos e configuração
A URL-base da ViaCEP SHALL ser configurável via variável de ambiente (`VIACEP_BASE_URL`) e NÃO SHALL ser fixada no código-fonte.

#### Scenario: Configuração ausente usa valor padrão documentado
- **WHEN** a variável `VIACEP_BASE_URL` não é definida no ambiente
- **THEN** o sistema usa o valor padrão documentado no `README.md`