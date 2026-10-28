## Why

O sistema hoje é uma API REST pura (consumida via Swagger/Postman), sem interface visual, sem autenticação e sem o conceito de carteira de investimentos — capacidades listadas como diferenciais no enunciado do trabalho (seção 11: "dashboard simples", "associação entre corretoras e carteiras de ações", "autenticação com Spring Security") e explicitamente marcadas como fora do escopo do MVP em `openspec/changes/criar-sistema-gestao-acoes/proposal.md`. O grupo decidiu perseguir esses diferenciais para a apresentação prática, usando a experiência de carteira do Investidor10 (investidor10.com.br) como referência visual e funcional: um dashboard de posições consolidadas, tela de lançamentos (aportes), proventos e gráficos simples.

## What Changes

- Adicionar **autenticação** ao backend: cadastro e login de usuário, emissão/validação de JWT, endpoints protegidos por usuário autenticado para os recursos novos de carteira e proventos.
- Adicionar nova capability de **Carteira**: lançamentos (registro de compra/aporte de uma ação por um usuário, numa corretora, com quantidade/preço/data) e posições consolidadas por ativo (quantidade total, preço médio, valor investido, valor atual, variação), calculadas a partir dos lançamentos do usuário.
- Adicionar nova capability de **Proventos**: registro e listagem de proventos/dividendos recebidos por ativo, associados ao usuário.
- Criar um **frontend Angular** (projeto novo, standalone components) que se torna a interface principal do sistema, com:
  - Tela de login e cadastro de usuário, rotas protegidas por guard de autenticação.
  - Dashboard "Resumo" com as posições consolidadas da carteira do usuário logado.
  - Tela de "Lançamentos" para registrar compras/aportes.
  - Tela de "Proventos" para registrar e listar dividendos recebidos.
  - Gráficos simples de alocação por ativo e evolução do patrimônio.
  - Telas de cadastro/listagem/busca de **Corretoras** e **Ações** (CRUD que hoje só existe via API/Swagger).
  - Layout inspirado no dashboard do Investidor10 (navegação por abas/sidebar, cards de resumo, tabela de posições).
- Os endpoints REST existentes de Corretoras e Ações (RF01–RF12 do change `criar-sistema-gestao-acoes`) **não mudam de contrato** e permanecem sem exigência de autenticação — decisão registrada em `design.md` para não quebrar a suíte de testes e a coleção Postman já entregues; o Angular exige login na experiência do usuário mesmo consumindo esses endpoints públicos.

## Capabilities

### New Capabilities

- `autenticacao`: cadastro de usuário, login, emissão/validação de JWT, proteção de rotas/endpoints por usuário autenticado.
- `gestao-carteira`: registro de lançamentos (compras/aportes) e cálculo/consulta de posições consolidadas por ativo, escopados por usuário.
- `gestao-proventos`: registro e listagem de proventos/dividendos recebidos por ativo, escopados por usuário.
- `interface-web`: aplicação Angular (telas, navegação, autenticação de UI, formulários e visualizações) que consome as APIs do backend — Resumo, Lançamentos, Proventos, Gráficos, e CRUD de Corretoras/Ações.

### Modified Capabilities

_(nenhuma — os endpoints existentes de `gestao-acoes` e `gestao-corretoras` não mudam de contrato nem de regra de negócio; apenas passam a ser consumidos também pelo novo frontend)_

## Impact

- **Código novo (backend)**: entidades `Usuario`, `Lancamento`, `Posicao` (ou calculada em runtime — decisão em `design.md`), `Provento`; novos controllers/services/repositories/DTOs; configuração de segurança (Spring Security + JWT); migrations Flyway novas (PostgreSQL, MySQL) e ajuste do schema H2/test.
- **Código novo (frontend)**: projeto Angular completo (novo diretório, ex. `frontend/`), com roteamento, guards, services HTTP, componentes de tela, estilos.
- **Dependências novas (backend)**: `spring-boot-starter-security`, biblioteca JWT (ex. `jjwt` ou `nimbus-jose-jwt`).
- **Dependências novas (frontend)**: Angular CLI/framework, cliente HTTP, biblioteca de gráficos leve (ex. Chart.js/ng2-charts ou similar).
- **Banco de dados**: novas tabelas (`usuario`, `lancamento`, `provento`, e `posicao` se persistida) com migrations versionadas nos três perfis (H2/test via sintaxe PostgreSQL, PostgreSQL dev, MySQL).
- **Testes e documentação**: novos testes unitários/integração para autenticação, carteira e proventos; atualização do README, `.env.example`, coleção Postman (com fluxo de login) e diagrama de entidades.
- **Sem impacto nos RF01–RF12 existentes**: contratos de `/corretoras` e `/acoes` permanecem os mesmos; nenhuma migration ou entidade existente é alterada de forma destrutiva.