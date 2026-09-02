# Sistema de Gestão de Ações

API REST acadêmica para cadastro e consulta de corretoras e ações, com dados validados/enriquecidos por integrações externas reais (CNPJ, CEP, cotações BR/US), autenticação de usuário e gestão de carteira (lançamentos, posições e proventos). Desenvolvida com Specification-Driven Development via [OpenSpec](https://github.com/Fission-AI/OpenSpec) — a especificação completa (proposta, specs por capacidade, design técnico e plano de tarefas) está em `openspec/changes/criar-sistema-gestao-acoes/` (MVP original) e `openspec/changes/adicionar-carteira-auth-frontend-angular/` (autenticação, carteira, proventos e frontend Angular).

Um frontend Angular consome esta API — ver [`frontend/README.md`](frontend/README.md) para instruções de execução.

## Aderência ao enunciado do trabalho

### Requisitos funcionais

| # | Requisito | Status | Onde |
|---|---|---|---|
| RF01 | Cadastrar corretora a partir do CNPJ | ✅ | `POST /corretoras`, `CorretoraService.registrar` |
| RF02 | Consultar dados cadastrais da corretora em API externa | ✅ | BrasilAPI CNPJ (`CnpjDataPort`/`BrasilApiCnpjAdapter`) |
| RF03 | Validar se a corretora é instituição compatível com o mercado financeiro | ✅ | BrasilAPI CVM (`InstituicaoFinanceiraPort`/`BrasilApiCvmAdapter`) |
| RF04 | Consultar e preencher endereço a partir do CEP | ✅ | ViaCEP (`EnderecoPort`/`ViaCepAdapter`) |
| RF05 | Listar corretoras cadastradas | ✅ | `GET /corretoras` (paginado) |
| RF06 | Buscar corretora por id e por CNPJ | ✅ | `GET /corretoras/{id}`, `GET /corretoras/cnpj/{cnpj}` |
| RF07 | Cadastrar ação informando ticker e mercado | ✅ | `POST /acoes`, `AcaoService.registrar` |
| RF08 | Consultar cotação da ação em API externa apropriada | ✅ | brapi.dev (BR) / Twelve Data ou Alpha Vantage (US), via `CotacaoStrategyResolver` |
| RF09 | Listar ações cadastradas | ✅ | `GET /acoes` (paginado) |
| RF10 | Buscar ação por ticker | ✅ | `GET /acoes/ticker/{ticker}` |
| RF11 | Atualizar a cotação de uma ação já cadastrada | ✅ | `PUT /acoes/{id}/atualizar-cotacao` |
| RF12 | Impedir cadastro duplicado de corretora por CNPJ e de ação por ticker | ✅ | `CorretoraDuplicadaException`/`AcaoDuplicadaException` → `409` |

### Regras de negócio

| # | Regra | Status | Observação |
|---|---|---|---|
| RN01 | CNPJ só é aceito se válido em formato e existir na base consultada | ✅ | Dígito verificador (`CnpjUtils`) + existência via BrasilAPI |
| RN02 | Dados principais vêm da consulta externa, não de preenchimento manual | ✅ | `razaoSocial`, `nomeFantasia`, `situacaoCadastral`, endereço sempre vêm da API |
| RN03 | Instituição não validada na CVM: cadastro é impedido | ✅ (decisão do grupo) | `InstituicaoNaoValidadaException` → `422`; trade-off documentado em `openspec/changes/criar-sistema-gestao-acoes/design.md` |
| RN04 | CEP validado em API pública antes de salvar | ✅ | ViaCEP; CEP inexistente → `422` |
| RN05 | Ação só é cadastrada se o ticker existir na API de cotação | ✅ | Ticker inexistente → `404` (`TickerNaoEncontradoException`) |
| RN06 | Sistema distingue ativos BR e US, direcionando à API adequada | ✅ | Enum `Mercado` + `CotacaoStrategyResolver` (Strategy Pattern) |
| RN07 | Não permite duas ações com o mesmo ticker | ✅ | Ticker único globalmente (índice único + verificação no service) |

### Entidades

- **Corretora**: `cnpj`, `razaoSocial`, `nomeFantasia`, `email`, `telefone`, `cep`, `logradouro`, `numero`, `complemento`, `bairro`, `cidade`, `uf`, `situacaoCadastral`, `validadaCvm`, `criadoEm`.
- **Acao**: `ticker`, `nomeEmpresa`, `mercado`, `moeda`, `cotacaoAtual`, `dataHoraCotacao`, `provedorOrigem`, `criadoEm`.
- Entidades adicionais (fora do mínimo pedido, suporte aos diferenciais de carteira/autenticação): `Usuario`, `Lancamento` (associa `Usuario` + `Acao` + `Corretora`) e `Provento`.

### Diferenciais implementados (seção 11 do enunciado)

| Diferencial | Status |
|---|---|
| Feign Client ou WebClient | ✅ Feign Client (`@FeignClient` em todos os adapters de integração) |
| Testes unitários e/ou de integração | ✅ 121 testes (`@DataJpaTest`, `@WebMvcTest`, integração ponta a ponta, `MockWebServer`) |
| Paginação nas listagens | ✅ `Pageable`/`Page` em corretoras, ações, lançamentos e proventos |
| Logs estruturados | ✅ `CorrelationIdFilter` + padrão de log com `correlationId` |
| Cache para consultas externas | ❌ Não implementado |
| Dashboard simples (Thymeleaf ou frontend separado) | ✅ Frontend Angular separado (`frontend/`) |
| Associação entre corretoras e carteiras de ações | ✅ `Lancamento` associa corretora, ação e usuário |
| Histórico de cotações | ❌ Não implementado (fora de escopo, ver "Limitações conhecidas") |
| Autenticação com Spring Security | ✅ JWT (`/auth/registrar`, `/auth/login`, filtro `JwtAuthenticationFilter`) |

### Restrições (seção 14 do enunciado)

- **Mínimo de 3 integrações externas reais**: são 5 — BrasilAPI (CNPJ), BrasilAPI (CVM), ViaCEP, brapi.dev e Twelve Data/Alpha Vantage.
- **Cenários de falha tratados**: API externa fora do ar/indisponível → `502` (`IntegracaoExternaIndisponivelException`); ticker inexistente → `404`; CNPJ inválido (formato/dígito) → `400`; CNPJ não encontrado na base → `404`; CEP inválido → `400`; CEP inexistente → `404`/`422`; todos centralizados no `GlobalExceptionHandler` (`ProblemDetail`). Validado manualmente contra os provedores reais (ver `docs/roteiro-apresentacao.md`).

### Entregáveis (seção 12 do enunciado)

- Código-fonte completo: este repositório.
- README com documentação das APIs usadas: seção [Integrações externas](#integrações-externas--limitações-e-observações-verificado-em-2026-08-12) abaixo.
- Coleção de testes: `postman/gestao-acoes.postman_collection.json`.
- Diagrama simplificado das entidades: `docs/diagramas.md`.
- Roteiro para a apresentação prática: `docs/roteiro-apresentacao.md`.

### Aderência às aulas de apoio (variáveis de ambiente, Liquibase e Docker)

O projeto segue os padrões ensinados em [`AULA-10-VARIAVEIS-DE-AMBIENTE`](https://github.com/jeffersonarpasserini/suporteos2025/blob/main/docs/aulas/AULA-10-VARIAVEIS-DE-AMBIENTE.md), [`AULA-11-LIQUIBASE`](https://github.com/jeffersonarpasserini/suporteos2025/blob/main/docs/aulas/AULA-11-LIQUIBASE.md) e [`AULA-12-DOCKER-E-CONTAINERS`](https://github.com/jeffersonarpasserini/suporteos2025/blob/main/docs/aulas/AULA-12-DOCKER-E-CONTAINERS.md), adaptados ao nome das variáveis nativas do Spring Boot (`SPRING_DATASOURCE_*` em vez de `DB_*`) e a três bancos em vez de um.

**Aula 10 — Variáveis de ambiente**

| Prática ensinada | Aplicada? |
|---|---|
| `.env.example` versionado, sem segredos reais | ✅ |
| `.gitignore` com `.env` / `.env.*` / `!.env.example` | ✅ |
| Placeholders `${VAR:padrao}` no `application.yml` | ✅ |
| Senha do banco sem valor padrão (`${SPRING_DATASOURCE_PASSWORD}`) — app não sobe sem ela | ✅ |
| Nenhum segredo real no histórico do Git | ✅ verificado (`git log --all` em `.env*`) |

**Aula 11 — Liquibase**

| Prática ensinada | Aplicada? |
|---|---|
| Changelog master + pasta `changes/` com changesets numerados | ✅ `db/changelog/db.changelog-master.xml` + `changes/001` a `006` |
| `spring.jpa.hibernate.ddl-auto=validate` (Hibernate só confere, não cria schema) | ✅ em todos os perfis |
| Changeset já executado nunca é editado; correção vira changeset novo | ✅ `006-fix-corretora-validada-cvm-mysql.xml` corrige o `005` sem alterá-lo |
| Mesmo changelog roda em H2 (testes) e no banco real | ✅ perfil `test` usa o mesmo master |
| Validado contra o banco de produção real do perfil (não só H2) | ✅ PostgreSQL sempre usado em `dev`; MySQL validado contra container real em 2026-09-01 (ver seção de perfis abaixo) |

**Aula 12 — Docker e Docker Compose**

| Prática ensinada | Aplicada? |
|---|---|
| `Dockerfile` multi-stage (build com Maven+JDK, runtime só com JRE) | ✅ |
| Usuário não-root no container (`spring:spring`) | ✅ |
| `.dockerignore` excluindo `.git`, `target`, `.env`, docs | ✅ |
| `compose.yaml` com `healthcheck` (`pg_isready`) no banco | ✅ |
| `depends_on: condition: service_healthy` (app só sobe com banco saudável) | ✅ |
| Volume nomeado para persistir dados do banco (`postgres_data`) | ✅ |
| `docker compose up/down/stop/start` preservando ou apagando dados conforme o comando | ✅ |
| Extensão além da aula: serviço `frontend` com proxy reverso, publicando uma única porta | ✅ (frontend Angular não fazia parte da aula original) |

## Pré-requisitos

- Java 17 (Eclipse Temurin recomendado)
- Maven (o wrapper `./mvnw` já está incluído, não é necessário instalar Maven)
- PostgreSQL 14+ rodando localmente (perfil `dev`, padrão) — **não é necessário** para rodar os testes, que usam H2 em memória
- IntelliJ IDEA (Community ou Ultimate)

## Configuração no IntelliJ IDEA

1. `File > Open...` e selecione a pasta raiz do projeto (contém `pom.xml`).
2. Aguarde o IntelliJ importar o projeto Maven (baixa as dependências automaticamente).
3. Configure o JDK do projeto para Java 17: `File > Project Structure > Project > SDK`.
4. Copie `.env.example` para um arquivo `.env` (ou configure as variáveis diretamente na *Run Configuration* do IntelliJ, aba **Environment variables**) — ver seção [Variáveis de ambiente](#variáveis-de-ambiente).
5. Rode a classe `GestaoAcoesApplication` (botão ▶ ao lado do `main`), ou use `./mvnw spring-boot:run` pelo terminal integrado.

## Perfis disponíveis

| Perfil | Banco | Uso |
|---|---|---|
| `dev` (padrão) | PostgreSQL (`SPRING_DATASOURCE_URL`) | Execução local persistente |
| `test` | H2 em memória (`MODE=PostgreSQL`) | Testes automatizados (ativado automaticamente pela suíte) |
| `mysql` | MySQL (`SPRING_DATASOURCE_URL`) | Alternativa persistente, exigida pelo enunciado ("banco de dados deverá ser H2, MySQL e PostgreSQL") |

O perfil ativo é controlado por `SPRING_PROFILES_ACTIVE`. O schema é criado/versionado por um único changelog Liquibase (`src/main/resources/db/changelog/db.changelog-master.xml`, incluindo os changesets em `changes/`) — cada changeset tem uma variante para PostgreSQL/H2 (`context=postgres-h2`, com `TIMESTAMP WITH TIME ZONE`) e outra para MySQL (`context=mysql`, com `TIMESTAMP(6)`/`DECIMAL`, sem suporte a timezone), selecionadas por `spring.liquibase.contexts` em cada `application-*.yml`. Nenhum perfil usa `ddl-auto=create`/`update`.

Para rodar com MySQL:
```powershell
# Crie o banco e o usuário primeiro (num cliente MySQL):
# CREATE DATABASE gestao_acoes;
# CREATE USER 'gestao_acoes'@'localhost' IDENTIFIED BY 'gestao_acoes';
# GRANT ALL PRIVILEGES ON gestao_acoes.* TO 'gestao_acoes'@'localhost';

$env:SPRING_PROFILES_ACTIVE = "mysql"
./mvnw spring-boot:run
```

**Validação**: além do `MysqlMigrationSmokeTest` (H2 em modo de compatibilidade MySQL, `MODE=MySQL`), o perfil `mysql` foi validado em 2026-09-01 contra um MySQL 8.4 real (container `mysql:8.4`): os 8 changeSets do Liquibase (contexto `mysql`) executaram com sucesso, o Hibernate validou o schema (`ddl-auto=validate`) sem divergências, e um fluxo completo de cadastro de corretora — incluindo as chamadas reais a BrasilAPI (CNPJ + CVM) e ViaCEP — gravou e leu os dados corretamente, com o booleano `validada_cvm` (tipo `BIT` no MySQL, ajustado pelo changeSet 006) preservado.

## Variáveis de ambiente

Ver `.env.example` para a lista completa. Resumo:

| Variável | Obrigatória | Descrição |
|---|---|---|
| `SPRING_DATASOURCE_URL`, `_USERNAME`, `_PASSWORD` | Sim (perfis `dev` e `mysql`) | Conexão com o banco. `_PASSWORD` **não tem valor padrão** — a aplicação falha ao subir se não for definida, de propósito: não deve iniciar silenciosamente com uma senha conhecida por todos |
| `APP_MARKET_DATA_US_PROVIDER` | Não (padrão `twelve-data`) | `twelve-data` ou `alpha-vantage` |
| `BRASIL_API_BASE_URL` | Não (tem padrão público) | Dados de CNPJ e validação CVM |
| `VIACEP_BASE_URL` | Não (tem padrão público) | Consulta de CEP |
| `BRAPI_BASE_URL`, `BRAPI_TOKEN` | Token opcional para os tickers `PETR4`, `MGLU3`, `VALE3`, `ITUB4`; obrigatório para os demais | Cotações do mercado brasileiro |
| `TWELVE_DATA_BASE_URL`, `TWELVE_DATA_API_KEY` | Chave obrigatória se `us-provider=twelve-data` | Cotações do mercado americano |
| `ALPHA_VANTAGE_BASE_URL`, `ALPHA_VANTAGE_API_KEY` | Chave obrigatória se `us-provider=alpha-vantage` | Cotações do mercado americano (alternativa) |
| `JWT_SECRET` | Não (tem padrão só para dev — troque em qualquer ambiente real) | Segredo de assinatura dos tokens JWT (login) |
| `APP_JWT_EXPIRATION_MINUTES` | Não (padrão `120`) | Validade do token JWT |
| `APP_CORS_ALLOWED_ORIGINS` | Não (padrão `http://localhost:4200`) | Origem(ns) liberada(s) para o frontend Angular |

Nenhuma chave real está versionada no repositório; `.env.example` contém apenas placeholders.

## Executando a aplicação

```bash
# Perfil dev (requer PostgreSQL local acessível pelas variáveis acima)
./mvnw spring-boot:run

# Sem PostgreSQL disponível, suba com H2 apenas para exploração manual
./mvnw spring-boot:run -Dspring-boot.run.profiles=test
```

Após subir, a documentação interativa fica disponível em `http://localhost:8080/swagger-ui.html` e o OpenAPI JSON em `http://localhost:8080/v3/api-docs`.

Para usar a interface (não apenas a API), rode o frontend Angular em paralelo — outro terminal, outra porta:

```bash
cd frontend
npm install
npm start   # http://localhost:4200
```

Ver `frontend/README.md` para detalhes.

## Executando com Docker

Alternativa à instalação manual do Java, do PostgreSQL e do Node: backend, banco e frontend Angular sobem juntos via Docker Compose (`Dockerfile` + `compose.yaml` na raiz do projeto; `frontend/Dockerfile` para o Angular). Tudo fica atrás de uma única porta publicada — ver detalhes abaixo.

Pré-requisitos: Docker Desktop (Windows/macOS) ou Docker Engine + plugin Compose (Linux).

```bash
docker --version
docker compose version
```

Se ainda não existir um `.env` na raiz, copie o exemplo e ajuste os valores (principalmente `SPRING_DATASOURCE_PASSWORD` e `JWT_SECRET`):

```powershell
Copy-Item .env.example .env
```

Suba os serviços (constrói a imagem na primeira vez):

```bash
./mvnw test
docker compose up -d --build
```

Acompanhe os logs da aplicação:

```bash
docker compose logs -f aplicacao
```

Com `APP_PORT=8080` (padrão), o frontend Angular e a API ficam disponíveis na mesma porta: `http://localhost:8080` (frontend) e `http://localhost:8080/swagger-ui.html` (Swagger). O container `aplicacao` não publica porta própria no host — o Nginx do container `frontend` faz proxy reverso das rotas da API (`/auth`, `/acoes`, `/carteira`, `/corretoras`, `/proventos`, `/actuator`, `/swagger-ui`, `/v3/api-docs`) para ele pela rede interna do Compose. Se `POSTGRES_PORT`/`APP_PORT` estiverem ocupadas, altere-as no `.env` — a comunicação interna entre os containers continua em `postgres:5432` e `aplicacao:8080`.

Parar sem apagar dados / retomar / remover containers (mantendo o volume) / apagar também o banco:

```bash
docker compose stop
docker compose start
docker compose down            # preserva o volume postgres_data
docker compose down --volumes  # apaga também o banco
```

Após alterar código Java, reconstrua apenas o serviço da aplicação:

```bash
docker compose up -d --build aplicacao
```

Para trabalhar pela IDE mantendo só o banco em container:

```bash
docker compose up -d postgres
```

Nesse caso, no IntelliJ use `SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:${POSTGRES_PORT:-5432}/gestao_acoes` (ou o `.env` já usado para execução local), já que fora do Compose o hostname `postgres` não existe.

## Executando os testes

```bash
./mvnw test
```

A suíte completa (121 testes na versão atual: unitários, `@DataJpaTest`, `@WebMvcTest` e integração ponta a ponta) roda inteiramente sobre H2 e mocks HTTP locais (`MockWebServer`) — **nenhum teste faz chamada de rede real** às APIs externas, portanto a suíte não consome cotas de nenhum provedor. As fatias `@WebMvcTest` de `/carteira` e `/proventos` carregam a `SecurityFilterChain` real para exercitar `401` sem token; as demais desligam os filtros de servlet (`addFilters = false`) por testarem endpoints públicos.

**Nota de ambiente**: o perfil de testes é fixado por `src/test/resources/application.properties` (`spring.profiles.active=test`). Se os testes começarem a conectar em um PostgreSQL local em vez do H2 em memória, confira esse arquivo — algum editor pode corrompê-lo (ex.: autocomplete inserindo uma quebra de linha no meio de `spring.profiles.active`).

## Endpoints principais

| Método | Caminho | Autenticação | Descrição |
|---|---|---|---|
| `POST` | `/corretoras` | Não | Cadastra corretora (CNPJ validado + enriquecido via BrasilAPI, validado na CVM, endereço via ViaCEP) |
| `GET` | `/corretoras` | Não | Lista corretoras (paginado) |
| `GET` | `/corretoras/{id}` | Não | Busca corretora por ID |
| `GET` | `/corretoras/cnpj/{cnpj}` | Não | Busca corretora por CNPJ (com ou sem máscara) |
| `POST` | `/acoes` | Não | Cadastra ação (ticker validado e cotação obtida do provedor do mercado) |
| `GET` | `/acoes` | Não | Lista ações (paginado) |
| `GET` | `/acoes/{id}` | Não | Busca ação por ID |
| `GET` | `/acoes/ticker/{ticker}` | Não | Busca ação por ticker (globalmente único — RN07) |
| `PUT` | `/acoes/{id}/atualizar-cotacao` | Não | Atualiza a cotação de uma ação já cadastrada |
| `POST` | `/auth/registrar` | Não | Cadastra usuário (nome, email, senha) |
| `POST` | `/auth/login` | Não | Autentica e retorna um token JWT |
| `POST` | `/carteira/lancamentos` | **Sim** (Bearer JWT) | Registra uma compra/aporte (ação, corretora, quantidade, preço, data) |
| `GET` | `/carteira/lancamentos` | **Sim** | Lista os lançamentos do usuário autenticado (paginado) |
| `GET` | `/carteira/posicoes` | **Sim** | Posições consolidadas por ativo (quantidade, preço médio, valor investido/atual, variação) |
| `POST` | `/proventos` | **Sim** | Registra um provento (dividendo/JCP) recebido |
| `GET` | `/proventos` | **Sim** | Lista os proventos do usuário autenticado (paginado, mais recente primeiro) |

Os endpoints de corretoras e ações permanecem públicos (sem exigir login) — só carteira e proventos exigem `Authorization: Bearer {token}` obtido em `/auth/login`. Ver a decisão registrada em `openspec/changes/adicionar-carteira-auth-frontend-angular/design.md`.

Payloads, exemplos de sucesso/erro e todos os códigos HTTP estão documentados no Swagger UI (botão "Authorize" para informar o token JWT) e nas specs em `openspec/changes/criar-sistema-gestao-acoes/specs/` e `openspec/changes/adicionar-carteira-auth-frontend-angular/specs/`. Uma coleção Postman com exemplos prontos está em `postman/gestao-acoes.postman_collection.json`.

## Integrações externas — limitações e observações (verificado em 2026-08-12)

| Provedor | Autenticação | Limites conhecidos | Freshness / observações |
|---|---|---|---|
| **BrasilAPI** (`/cnpj/v1/{cnpj}`, `/cvm/corretoras/v1/{cnpj}`) | Nenhuma | Serviço público comunitário, sem SLA formal | Dados de CNPJ vêm da Receita Federal (cache do provedor); validação CVM tem endpoint de consulta direta por CNPJ (não é necessário baixar a lista completa) |
| **ViaCEP** | Nenhuma | Serviço público, sem SLA formal | CEP com formato inválido retorna `400` HTML (não JSON); CEP válido inexistente retorna `200` com `{"erro":"true"}` |
| **brapi.dev** | `BRAPI_TOKEN` opcional só para os tickers de demonstração (`PETR4`, `MGLU3`, `VALE3`, `ITUB4`); obrigatório para os demais | Plano gratuito com token tem limite de requisições — ver [brapi.dev/dashboard](https://brapi.dev) para o plano contratado | Cotação "quase em tempo real" durante o pregão; `regularMarketTime` vem em UTC |
| **Twelve Data** (provedor US padrão) | `TWELVE_DATA_API_KEY` obrigatória | Plano gratuito: limite baixo de requisições por minuto/dia (verificar plano em twelvedata.com/pricing) | Endpoint `/quote` usado; preço de fechamento mais recente do candle diário disponível no plano gratuito, não necessariamente tick-by-tick |
| **Alpha Vantage** (alternativa US, via `app.market-data.us-provider=alpha-vantage`) | `ALPHA_VANTAGE_API_KEY` obrigatória | Plano gratuito historicamente limitado a poucas requisições/minuto e por dia | `GLOBAL_QUOTE` retorna apenas a data do último pregão (`latest trading day`), sem horário — a aplicação assume `00:00:00 UTC` nesse dia, documentado como limitação |

Este projeto usa exclusivamente planos gratuitos/públicos desses provedores; nenhum endpoint premium é requisito do MVP.

## Limitações conhecidas do MVP

Fora de escopo (ver `proposal.md`/`design.md` de `openspec/changes/adicionar-carteira-auth-frontend-angular/`, seção "Non-Goals"): venda/baixa de posição (carteira só registra compras/aportes), refresh token/OAuth2/login social, autorização por papéis (roles/admin), histórico de cotações, cache avançado, circuit breaker, fallback automático entre provedores US, métricas/tracing, filtros de busca além de paginação simples.

## Estrutura do projeto

```
src/main/java/br/com/gestaoacoes/
  controller/     endpoints REST
  service/        regras de negócio e orquestração
  repository/     Spring Data JPA
  model/          entidades JPA e enums
  dto/            payloads de entrada/saída (nunca as entidades diretamente)
  mapper/         entidade -> DTO
  exception/      exceções de domínio + handler global (ProblemDetail)
  config/         filtros, segurança (Spring Security) e configuração transversal
  security/       JWT (geração/validação, filtro de autenticação, handlers de erro)
  integration/    portas + adaptadores por provedor externo (Ports and Adapters)

frontend/         aplicação Angular (interface do sistema) — ver frontend/README.md
```