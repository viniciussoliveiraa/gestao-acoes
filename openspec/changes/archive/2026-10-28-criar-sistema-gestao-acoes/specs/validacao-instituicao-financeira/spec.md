## Purpose

Verifica, a partir de fonte pública da CVM (via BrasilAPI ou equivalente documentado), se um CNPJ corresponde a uma instituição compatível com atuação no mercado financeiro, aplicando a política de rejeição definida em RN03 para instituições não reconhecidas.

## ADDED Requirements

### Requirement: Porta interna de validação de instituição financeira
O sistema SHALL definir uma porta interna para validação de instituição financeira por CNPJ, consumida pelo serviço de corretoras, e um adaptador concreto que consulta o recurso de corretoras da CVM exposto pela BrasilAPI (ou fonte equivalente documentada em `design.md`).

#### Scenario: Instituição reconhecida
- **WHEN** o CNPJ normalizado consta como corretora ativa na fonte consultada
- **THEN** o adaptador retorna "instituição validada" para a camada de serviço

### Requirement: Política de rejeição para instituição não validada
Quando o CNPJ não constar como instituição financeira reconhecida na fonte consultada, o sistema SHALL rejeitar o cadastro da corretora (RN03), respondendo `422` e não persistindo nenhum dado.

#### Scenario: Instituição não reconhecida
- **WHEN** o CNPJ é válido e existe na BrasilAPI, mas não consta na fonte da CVM como corretora ativa
- **THEN** o adaptador retorna "instituição não validada" e o serviço de corretoras responde `422` sem persistir a corretora

### Requirement: Custo e cache da consulta à lista da CVM
Se a validação exigir obter uma lista completa de instituições e filtrar localmente, o sistema SHALL documentar em `design.md` o custo, a estratégia de cache (com TTL) e os limites de uso, e NÃO SHALL realizar uma varredura completa da lista a cada requisição de cadastro sem cache.

#### Scenario: Consulta repetida em curto intervalo usa cache
- **GIVEN** a lista de corretoras da CVM foi obtida e armazenada em cache dentro do TTL configurado
- **WHEN** uma nova requisição de cadastro precisa validar outro CNPJ dentro do mesmo TTL
- **THEN** o adaptador reutiliza os dados em cache em vez de repetir a chamada completa ao provedor

### Requirement: Resiliência da integração
O adaptador SHALL aplicar timeout de conexão e leitura configuráveis. Falha de infraestrutura na validação NÃO SHALL ser confundida com "instituição não validada": o sistema SHALL responder `502`/`503` para indisponibilidade do provedor, e `422` apenas quando a instituição de fato não for encontrada na fonte consultada.

#### Scenario: Provedor de validação indisponível
- **WHEN** a fonte de validação da CVM está indisponível ou expira por timeout
- **THEN** o sistema responde `502` ou `503`, distinto da resposta `422` usada para instituição não reconhecida, e nenhuma corretora é persistida