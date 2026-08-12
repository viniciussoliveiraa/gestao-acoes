## Why

Não existe hoje nenhuma API para cadastrar corretoras e ativos financeiros com dados validados contra fontes externas reais (Receita/CNPJ, CVM, ViaCEP, provedores de cotação BR/US). O projeto acadêmico "Sistema de Gestão de Ações" precisa demonstrar arquitetura em camadas, isolamento de fornecedores externos (Ports and Adapters), tratamento de erro centralizado e persistência confiável — construídos do zero, com Specification-Driven Development, para que decisões de escopo e regras de negócio fiquem registradas e aprovadas antes de qualquer código.

## What Changes

- Criar o projeto Spring Boot (Java 17, Maven) do zero, com arquitetura em camadas (`controller`, `service`, `repository`, `model`, `dto`, `mapper`, `exception`, `config`, `integration`).
- Adicionar cadastro e consulta de **corretoras**: validação matemática de CNPJ, enriquecimento via BrasilAPI (dados cadastrais), validação de instituição financeira via CVM/BrasilAPI, preenchimento de endereço via ViaCEP, unicidade de CNPJ (normalizado).
- Adicionar cadastro e consulta de **ações** brasileiras e americanas: normalização de ticker/mercado, roteamento por Strategy para o provedor de cotação correto (brapi.dev para BR; Twelve Data ou Alpha Vantage para US, selecionável por configuração), unicidade por `(ticker, mercado)`, atualização de cotação sob demanda.
- Isolar todo fornecedor externo atrás de portas internas (uma por capacidade: CNPJ, CEP, validação de instituição, cotação BR, cotação US) com um adaptador por provedor, seguindo Ports and Adapters/Strategy.
- Adicionar tratamento de erro centralizado no formato Problem Details (RFC 9457), com mapeamento de erros de validação, duplicidade (409), recurso inexistente (404) e indisponibilidade de dependência externa (502/503).
- Adicionar resiliência básica: timeouts, retry limitado a falhas transitórias/idempotentes, tratamento de rate limit (429), sem persistência parcial em caso de falha de provedor.
- Adicionar documentação: OpenAPI/Swagger, README com variáveis de ambiente e limitações de cada API externa, `.env.example`, coleção Postman/Insomnia, diagramas de entidades e de componentes.
- Definir suíte de testes que não realiza chamadas reais a APIs externas (mocks/MockWebServer/WireMock) e testes de integração com banco isolado (Testcontainers/H2, conforme perfil).
- **Fora de escopo do MVP** (registrado como diferenciais futuros, não implementados nesta mudança): histórico de cotações, entidade `Carteira` e suas associações, dashboard/frontend, autenticação/autorização (Spring Security), cache avançado, circuit breaker, fallback automático entre provedores US, métricas/tracing, paginação/filtros avançados além do mínimo.

## Capabilities

### New Capabilities

- `gestao-corretoras`: cadastro, listagem (paginada), busca por ID e por CNPJ de corretoras, com unicidade de CNPJ normalizado.
- `gestao-acoes`: cadastro, listagem (paginada), busca por ID e por ticker/mercado de ações, e atualização sob demanda da cotação.
- `integracao-dados-empresariais`: adaptador de consulta de dados cadastrais por CNPJ via BrasilAPI, isolado atrás de uma porta interna.
- `integracao-enderecos`: adaptador de consulta e validação de CEP via ViaCEP, isolado atrás de uma porta interna.
- `validacao-instituicao-financeira`: verificação de que uma corretora é uma instituição compatível com atuação no mercado financeiro (fonte CVM/BrasilAPI), com política de rejeição para instituições não reconhecidas.
- `integracao-cotacoes`: seleção de adaptador de cotação por mercado (brapi.dev para BR; Twelve Data/Alpha Vantage para US, via Strategy/Factory), obtenção e persistência de cotação com precisão monetária e timezone corretos.
- `tratamento-erros-resiliencia`: formato de erro centralizado (Problem Details), timeouts, retry limitado, tratamento de 429, garantia de não persistência parcial, tradução de violações de unicidade concorrentes para 409.
- `documentacao-observabilidade`: OpenAPI/Swagger, logs estruturados sem dados sensíveis, README reproduzível, artefatos de apresentação (Postman/Insomnia, diagramas).

### Modified Capabilities

_(nenhuma — projeto novo, sem specs existentes para modificar)_

## Impact

- **Código novo**: todo o projeto Spring Boot (não existe implementação prévia além do esqueleto gerado pelo Spring Initializr).
- **Dependências novas**: Spring Web, Spring Data JPA, Bean Validation, cliente HTTP (Feign ou WebClient — decisão em `design.md`), springdoc-openapi, driver H2/PostgreSQL (e MySQL se exigido), Flyway, JUnit 5, Mockito, Spring Boot Test, MockWebServer/WireMock, Testcontainers.
- **Integrações externas**: BrasilAPI (CNPJ e CVM), ViaCEP, brapi.dev, Twelve Data, Alpha Vantage — todas via variáveis de ambiente/configuração, nunca hardcoded.
- **Banco de dados**: schema novo via Flyway, com constraints de unicidade para CNPJ e `(ticker, mercado)`.
- **Sem impacto em sistemas existentes**: repositório novo, sem consumidores anteriores da API.
