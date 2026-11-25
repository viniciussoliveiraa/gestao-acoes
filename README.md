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
| RN04 | CEP validado em API pública antes de salvar | ✅ | ViaCEP; CEP inexistente → `404` (`CepNaoEncontradoException`) |
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
| Testes unitários e/ou de integração | ✅ 226 testes (`@DataJpaTest`, `@WebMvcTest`, integração ponta a ponta, `MockWebServer`), com portão de cobertura JaCoCo (`./mvnw clean verify`) |
| Paginação nas listagens | ✅ `Pageable`/`Page` em corretoras, ações, lançamentos e proventos |
| Logs estruturados | ✅ `CorrelationIdFilter` + padrão de log com `correlationId` |
| Cache para consultas externas | ❌ Não implementado |
| Dashboard simples (Thymeleaf ou frontend separado) | ✅ Frontend Angular separado (`frontend/`) |
| Associação entre corretoras e carteiras de ações | ✅ `Lancamento` associa corretora, ação e usuário |
| Histórico de cotações | ❌ Não implementado (fora de escopo, ver "Limitações conhecidas") |
| Autenticação com Spring Security | ✅ JWT (`/auth/registrar`, `/auth/login`, filtro `JwtAuthenticationFilter`) |

### Restrições (seção 14 do enunciado)

- **Mínimo de 3 integrações externas reais**: são 5 — BrasilAPI (CNPJ), BrasilAPI (CVM), ViaCEP, brapi.dev e Twelve Data/Alpha Vantage.
- **Cenários de falha tratados**: API externa fora do ar/indisponível → `502` (`IntegracaoExternaIndisponivelException`); ticker inexistente → `404`; CNPJ inválido (formato/dígito) → `400`; CNPJ não encontrado na base → `404`; CEP inválido (formato) → `400`; CEP inexistente → `404`; instituição não validada na CVM → `422`; todos centralizados no `GlobalExceptionHandler` (`ProblemDetail`). Validado manualmente contra os provedores reais (ver `docs/roteiro-apresentacao.md`).

### Entregáveis (seção 12 do enunciado)

- Código-fonte completo: este repositório.
- README com documentação das APIs usadas: seção [Integrações externas](#integrações-externas--limitações-e-observações-verificado-em-2026-08-12) abaixo.
- Coleção de testes: `postman/gestao-acoes.postman_collection.json`.
- Diagrama simplificado das entidades: `docs/diagramas.md`.
- Roteiro para a apresentação prática: `docs/roteiro-apresentacao.md`.

### Aderência às aulas de apoio (variáveis de ambiente, Liquibase e Docker)

O projeto segue os padrões ensinados em [`AULA-10-VARIAVEIS-DE-AMBIENTE`](https://github.com/jeffersonarpasserini/suporteos2025/blob/main/docs/aulas/AULA-10-VARIAVEIS-DE-AMBIENTE.md), [`AULA-11-LIQUIBASE`](https://github.com/jeffersonarpasserini/suporteos2025/blob/main/docs/aulas/AULA-11-LIQUIBASE.md) e [`AULA-12-DOCKER-E-CONTAINERS`](https://github.com/jeffersonarpasserini/suporteos2025/blob/main/docs/aulas/AULA-12-DOCKER-E-CONTAINERS.md), adaptados ao nome das variáveis nativas do Spring Boot (`SPRING_DATASOURCE_*` em vez de `DB_*`) e a três bancos em vez de um.

**Prova rápida para o professor**: cada aula tem sua tag no Git, exatamente como pedido no "Ponto de versionamento" de cada aula — `git tag -l` lista `aula-10-variaveis-ambiente`, `aula-11-liquibase` e `aula-12-docker`, cada uma no commit em que aquela aula foi aplicada. Um roteiro de demonstração ao vivo (comandos prontos para copiar/colar) está em [`docs/roteiro-apresentacao.md`](docs/roteiro-apresentacao.md#0-provas-de-aderência-às-aulas-de-apoio-variáveis-de-ambiente-liquibase-docker), seção 0.

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

## Aderência às aulas de login JWT, controle de acesso e testes/cobertura

As aulas [`AULA-13-LOGIN-COM-JWT`](https://github.com/jeffersonarpasserini/suporteos2025/blob/main/docs/aulas/AULA-13-LOGIN-COM-JWT.md), [`AULA-14-CONTROLE-DE-ACESSO`](https://github.com/jeffersonarpasserini/suporteos2025/blob/main/docs/aulas/AULA-14-CONTROLE-DE-ACESSO.md) e [`AULA-15-TESTES-E-COBERTURA`](https://github.com/jeffersonarpasserini/suporteos2025/blob/main/docs/aulas/AULA-15-TESTES-E-COBERTURA.md) usam como exemplo o projeto `suporteos2025` (CRUD de grupos/produtos). Como o gestao-acoes já tinha autenticação JWT desde a mudança `adicionar-carteira-auth-frontend-angular` (ver seção de endpoints acima), aplicá-las significou: (Aula 13) trocar a implementação por `JwtEncoder`/`JwtDecoder` nativos do Spring Security, como a aula ensina, em vez de uma lib externa de JWT; (Aula 14) adicionar de fato o RBAC que antes não existia; (Aula 15) adicionar o portão de cobertura JaCoCo que antes não existia. Tag no Git de cada aula: `aula-13-login-jwt`, `aula-14-controle-acesso`, `aula-15-testes-cobertura`.

**Aula 13 — Login com JWT**

| Prática ensinada | Aplicada? |
|---|---|
| Senha armazenada só como hash BCrypt, nunca em texto puro | ✅ (já valia desde `adicionar-carteira-auth-frontend-angular`; mantido) |
| JWT assinado com HS256 via `JwtEncoder`/`JwtDecoder` do próprio Spring Security (sem lib externa) | ✅ `SecurityConfig` (beans `jwtEncoder`/`jwtDecoder`/`jwtSecretKey`) — substitui a implementação anterior baseada em `jjwt`, que era funcionalmente equivalente mas não usava a API nativa do Spring Security exigida pela aula |
| Propriedades do JWT tipadas e validadas na inicialização (`JwtProperties`) | ✅ falha cedo se `secret` tiver menos de 32 bytes, `expiration-minutes` não for positivo, ou `issuer` estiver em branco |
| API stateless (`SessionCreationPolicy.STATELESS`) | ✅ (mantido) |
| Login via `AuthenticationManager`/`DaoAuthenticationProvider` (não comparação manual de senha) | ✅ `AuthService.login` — `UsuarioDetailsService` + `PasswordEncoder` compõem o provider automaticamente |
| `GET /api/auth/me` (equivalente: `GET /auth/me`) | ✅ devolve o usuário do token autenticado |
| Distinção clara `401` (sem token/inválido/expirado) vs `403` (autenticado sem permissão) | ✅ ver Aula 14 |
| Administrador inicial criado no startup, só nos perfis não-`test` | ✅ `AdminInitializer` (perfis `dev`/`mysql`), `APP_ADMIN_EMAIL`/`APP_ADMIN_PASSWORD` sem valor padrão (mesma filosofia de `SPRING_DATASOURCE_PASSWORD`) |

**Aula 14 — Controle de acesso (RBAC)**

Matriz de acesso aplicada ao domínio do gestao-acoes (diferente do `suporteos2025`: aqui corretoras/ações já eram um catálogo público por exigência do enunciado original — RF01-RF12 —, então a aula foi adaptada para não quebrar isso; carteira/proventos já eram por usuário, não por admin):

| Recurso ou operação | Público | `USER` | `ADMIN` |
|---|:---:|:---:|:---:|
| `POST /auth/login`, `POST /auth/registrar` | Sim | Sim | Sim |
| `GET /auth/me` | Não | Sim | Sim |
| Consultar corretoras/ações (`GET`) | Sim | Sim | Sim |
| Cadastrar/atualizar corretoras/ações | Não | Não | Sim |
| Lançamentos e proventos (próprios) | Não | Sim | Sim |
| Gerenciar usuários (`/usuarios`) | Não | Não | Sim |

| Prática ensinada | Aplicada? |
|---|---|
| `@EnableMethodSecurity` + `@PreAuthorize("hasRole('ADMIN')")` | ✅ em `SecurityConfig` e nos métodos de escrita de `CorretoraController`/`AcaoController`, e na classe `UsuarioController` inteira |
| Papéis (`ADMIN`/`USER`) persistidos e versionados pelo Liquibase | ✅ `Usuario.papel`/`Usuario.ativo`, changeset `007-add-papel-ativo-usuario.xml` (não altera os changesets já aplicados) |
| Contratos de usuário sem expor hash de senha | ✅ `UsuarioResponse` nunca inclui `senha`/hash; `UsuarioCreateRequest`/`UsuarioUpdateRequest`/`AlterarSenhaRequest` separam cadastro, alteração cadastral e troca de senha |
| Desativar em vez de excluir (auditoria) | ✅ não há `DELETE /usuarios/{id}`; `PUT /usuarios/{id}` com `ativo=false` desativa (bloqueia logins futuros; tokens já emitidos continuam válidos até expirar, mesma ressalva da aula) |
| Testes demonstrando `200`/`401`/`403` | ✅ `UsuarioFluxoIntegrationTest` (equivalente ao `UsuarioResourceIntegrationTest` da aula: hash BCrypt gravado, respostas sem senha/hash, alteração de papel/situação persistida, troca de senha com a antiga deixando de autenticar, `@WithMockUser` e JWT real), `UsuarioControllerTest`, `CorretoraFluxoIntegrationTest`/`AcaoFluxoIntegrationTest` (cadastro sem token → `401`; com `USER` → `403`; com `ADMIN` → `201`) |
| Último administrador ativo não pode ser rebaixado/desativado (seção 12.1) | ✅ `UsuarioService` responde `422` (`UltimoAdministradorException`) — implementado em TDD |

**Aula 15 — Testes, TDD e cobertura (JaCoCo)**

| Prática ensinada | Aplicada? |
|---|---|
| `jacoco-maven-plugin` com `prepare-agent`/`report`/`check` no `pom.xml` | ✅ |
| Portão de cobertura elevado só quando a suíte sustenta | ✅ `jacoco.minimum.line.coverage=0.85`, `jacoco.minimum.branch.coverage=0.85` (acima da meta acadêmica de 80%). Linha de base: 96,35% linhas / 77,54% ramos com portão de ramos em `0.70`; medição atual: **96,86% linhas (834/861), 87,50% ramos (133/152)** |
| `./mvnw clean verify` gera relatório e aplica o portão | ✅ relatórios em `target/site/jacoco/index.html` (HTML), `jacoco.xml`, `jacoco.csv` |
| Build falha se a cobertura cair abaixo do portão | ✅ verificado com `-Djacoco.minimum.branch.coverage=0.90` (226 testes passam, build falha no `check`, pois o real é 87,50%) e confirmando que `./mvnw clean verify` sem overrides volta a passar |
| Testes novos cobrindo as áreas antes sem cobertura (Aulas 13/14) | ✅ `UsuarioServiceTest`, `UsuarioControllerTest`, `AdminInitializerTest`, `JwtDecoderValidationTest` (token adulterado/expirado/emissor errado), `JwtPrincipalConverterTest` |
| Ciclos TDD Red → Green → Refactor com evidência da falha | ✅ 3 ciclos em `UsuarioService`/DTOs (nome não era gravado na atualização — defeito real; último administrador; senha 8–72) — saídas em [`docs/evidencias-aulas-13-14-15.md`](docs/evidencias-aulas-13-14-15.md) |
| Testes parametrizados e valores de fronteira | ✅ `SenhaValidacaoTest` (7/8/72/73 caracteres), `JwtPropertiesTest` (segredo 31/32 bytes, expiração 0/1, emissor nulo/vazio/em branco), `CorrelationIdFilterTest` |
| Teste que detecta alteração proposital (mutação) | ✅ dois mutantes manuais (`<= 1`→`< 1` e `< 32`→`< 31`), ambos mortos — ver documento de evidências |
| Integração contínua | ✅ `.github/workflows/testes.yml` roda `./mvnw --batch-mode clean verify` e publica o relatório JaCoCo; um segundo job roda os testes e o portão de cobertura do frontend (`npm run test:coverage`) |
| Suíte cresceu sem reduzir o enunciado de nenhum teste existente | ✅ 226 testes (eram 170 antes desta etapa, 140 antes das aulas), 0 falhas, nenhuma exclusão no JaCoCo |

**Extensão além da aula — tela de administração de usuários (frontend)**

A Aula 14 entregou o gerenciamento de usuários só na API (`/usuarios`). Depois das aulas, o frontend ganhou a tela **Administração → Usuários** (`/usuarios`), que usa esses mesmos endpoints — nenhuma mudança no backend foi necessária:

| O que a tela faz | Endpoint usado |
|---|---|
| Lista todos os usuários (nome, email/login, papel, situação; o próprio admin aparece marcado como "você") | `GET /usuarios` |
| Altera o **login (email)**, nome, papel (`USER`/`ADMIN`) e situação (ativo/inativo) | `PUT /usuarios/{id}` |
| Define uma **nova senha** (8 a 72 caracteres, com confirmação) | `PUT /usuarios/{id}/senha` |

- O link no menu e a rota só aparecem/abrem para quem tem `ROLE_ADMIN` na claim `authorities` do JWT (`adminGuard`, `AuthService.administrador()`); um `USER` que digitar `/usuarios` volta para o Resumo. Isso é só conveniência de interface — quem garante a regra é o `@PreAuthorize` do backend (`403` para `USER`).
- Dados e senha são salvos por formulários separados, espelhando a separação `UsuarioUpdateRequest` × `AlterarSenhaRequest` do backend.
- Erros do backend aparecem na própria tela: email já usado por outro usuário (`409`), senha fora do tamanho (`400`), rebaixar/desativar o último administrador ativo (`422`).
- Ao trocar o próprio email, o admin é avisado para usar o novo email no próximo login (o token atual continua válido até expirar).
- Verificado no ambiente Docker: após trocar login e senha de um usuário, o login com os dados antigos devolve `401` e com os novos `200`.

Arquivos: `frontend/src/app/features/usuarios/` (tela), `core/services/usuario.service.ts`, `core/models/usuario.model.ts`, `core/guards/admin.guard.ts`, e ajustes em `core/services/auth.service.ts`, `app.routes.ts` e `features/shell/shell.html`.

**Extensão além da aula — modo noturno (frontend)**

Botão de lua/sol na barra do topo alterna a interface inteira entre modo claro e escuro. Só frontend, sem nenhuma mudança no backend:

- A escolha fica salva no navegador (`localStorage`, chave `gestao-acoes.tema`); sem escolha salva, segue a preferência do sistema operacional (`prefers-color-scheme`).
- Um script curto no `index.html` aplica o tema antes do primeiro desenho da página, para ela não "piscar" branca ao abrir no modo escuro.
- Como todas as telas já usavam as variáveis CSS `--app-*`, o modo escuro só redefine essas variáveis sob a classe `tema-escuro` no `<html>` (`styles.scss`); o Angular Material usa `theme-type: color-scheme`, então campos, selects e menus também escurecem.
- Os gráficos (Chart.js desenha em `<canvas>` e não enxerga variáveis CSS) têm uma paleta de texto/grade/bordas por tema em `graficos.ts`, e as opções viraram `computed`, então os gráficos se redesenham na hora da troca. A paleta categórica das séries é a mesma nos dois modos.

Arquivos: `core/services/tema.service.ts` (novo), `src/index.html`, `src/styles.scss`, `features/shell/` (botão) e `features/graficos/` (cores por tema).

**Extensão além da aula — testes e cobertura do frontend (Vitest)**

O JaCoCo mede só o backend (Java). Para o frontend Angular, o equivalente é o Vitest (executor de testes padrão do Angular CLI) com o provedor de cobertura `@vitest/coverage-v8`, aplicando as mesmas práticas da Aula 15:

| Prática da Aula 15 | No frontend |
|---|---|
| Relatório de cobertura | `npm run test:coverage` → `frontend/coverage/frontend/index.html` (HTML) + resumo no terminal + `lcov.info` |
| Contar o projeto inteiro, sem esconder arquivos | `coverageInclude: src/app/**/*.ts` no `angular.json` — telas sem teste entrariam no total como 0% |
| Portão que falha o comando se a cobertura cair | `coverageThresholds` 85% em statements, ramos, funções e linhas (`angular.json`) |
| Dublês no lugar das dependências | serviços simulados com `vi.fn()`, `HttpTestingController` para conferir as chamadas HTTP, diretiva falsa no lugar do gráfico (jsdom não tem `<canvas>`) |
| Valores de fronteira / parametrizados | `it.each` na força da senha, na saudação por horário, paginador (lista vazia, última página) |
| Mutação manual | trocar `ROLE_ADMIN` por `ROLE_USER` em `auth.service.ts` derruba 2 testes |

Medição atual: **165 testes, 99,81% statements, 97,00% ramos, 99,71% funções, 100% linhas**. Os ramos restantes são proteções `typeof window === 'undefined'` (código fora do navegador), que não ocorrem no uso real. Demonstração do portão falhando: `npx ng test --watch=false --coverage --exclude=src/app/features/graficos/graficos.spec.ts` (cobertura cai para ~79% e o comando termina com erro).

O que é testado: todas as telas (login, cadastro, menu/topo, Resumo, Lançamentos, Proventos, Gráficos, Reequilíbrio, Corretoras, Ações, Usuários), nos caminhos de sucesso e de erro do backend (`401`/`403`/`404`/`409`/`422`/`503`), e toda a pasta `core` (`AuthService`, guards, interceptor, serviços HTTP, modo noturno, mensagens de erro, paginador, rotas). Os testes não fazem chamadas reais à API e não entram no build de produção (`tsconfig.app.json` exclui `*.spec.ts`, `src/test-setup.ts` e `core/testing/`).

**Meta acadêmica declarada**: 80% linhas / 80% ramos — **atingida** (96,86% / 87,50%). Progressão do portão de ramos: `0.70` → `0.80` → `0.85`, cada passo elevado só depois de a nova suíte sustentá-lo. Justificativas, explicação `401` × `403`, regra horizontal, posição de cada `@PreAuthorize`, uso de dublês e evidências de TDD/mutação estão em [`docs/evidencias-aulas-13-14-15.md`](docs/evidencias-aulas-13-14-15.md).

## Todos os testes feitos nas Aulas 13, 14 e 15

Esta seção lista **todos** os testes criados ou reescritos durante as três aulas, separados pela
aula a que pertencem, com o que cada um verifica. São **74 métodos de teste** em 16 classes, que
geram **87 execuções**, porque os testes parametrizados rodam uma vez para cada valor. Dessas, 82
são testes novos; as outras 5 são testes antigos reescritos para a API nativa do Spring Security. A
suíte foi de 140 para **222 testes**, todos passando, com **96,52% de cobertura de linhas e 84,87%
de ramos**. Depois, 4 testes de `CnpjUtils`/`CepUtils` levaram a suíte a **226 testes**, com
**96,86% de linhas e 87,50% de ramos** (ver a seção "Evolução da cobertura e do portão JaCoCo").

Legenda: 🆕 classe nova · ✏️ classe que já existia e ganhou testes ou foi reescrita.

### Aula 13: login com JWT (34 execuções)

**🆕 `AuthFluxoIntegrationTest`**: integração com contexto Spring completo (banco H2, Liquibase e
segurança real, sem mocks). Faz login de verdade antes de cada cenário.

- `loginComCredenciaisValidasEmiteJwtAssinado`: login correto devolve `200` e um JWT no formato `header.payload.assinatura`.
- `loginComSenhaIncorretaRetorna401`: senha errada devolve `401`.
- `recursoProtegidoSemTokenRetorna401`: `/carteira/posicoes` e `/auth/me` sem token devolvem `401`.
- `recursoProtegidoComTokenEmitidoRetorna200`: o token recebido no login dá acesso a um recurso protegido (`200`).
- `tokenAdulteradoRetorna401`: trocar um caractere da assinatura faz o token ser recusado (`401`).
- `tokenExpiradoRetorna401`: token com assinatura válida, mas vencido, é recusado (`401`).
- `escritaComPapelUserRetorna403`: usuário `USER` tentando cadastrar corretora recebe `403`.
- `meDevolveOUsuarioDoTokenSemExporSenha`: `/auth/me` devolve id, email e papel, sem senha nem hash.

**🆕 `JwtDecoderValidationTest`**: testa direto o decodificador de JWT do Spring Security.

- `tokenValidoDecodificaComSucesso`: um token válido é aceito e os dados são lidos.
- `tokenComAssinaturaDiferenteNaoDecodifica`: um token assinado com outra chave é recusado.
- `tokenExpiradoNaoDecodifica`: um token vencido é recusado.
- `tokenComEmissorDiferenteDoEsperadoNaoDecodifica`: um token de outro emissor (`iss`) é recusado.
- `tokenMalformadoNaoDecodifica`: um texto que não é JWT é recusado.

**🆕 `JwtPropertiesTest`**: configurações do JWT validadas na inicialização, com valores de fronteira. São 7 métodos, que dão 10 execuções.

- `segredoComExatamente32BytesEhAceito`: um segredo de 32 bytes (256 bits, o mínimo do HS256) é aceito.
- `segredoCom31BytesEhRecusado`: um segredo de 31 bytes é recusado.
- `segredoAusenteEhRecusado`: um segredo nulo é recusado.
- `tamanhoDoSegredoEhMedidoEmBytesUtf8ENaoEmCaracteres`: 16 caracteres acentuados ocupam 32 bytes e são aceitos.
- `expiracaoNaoPositivaEhRecusada` (parametrizado: 0 e -1): expiração zero ou negativa é recusada.
- `expiracaoDeUmMinutoEhAceita`: 1 minuto, o menor valor válido, é aceito.
- `emissorAusenteOuEmBrancoEhRecusado` (parametrizado: nulo, vazio e em branco): emissor inválido é recusado.

**🆕 `JwtPrincipalConverterTest`**: conversão do token no usuário autenticado.

- `converteSubjectParaUsuarioIdELeAuthorities`: o `sub` vira o id do usuário e a claim `authorities` vira os papéis.
- `semClaimAuthoritiesResultaEmListaVazia`: um token sem papéis não quebra e resulta numa lista vazia.

**✏️ `JwtServiceTest`**: reescrito para a API nativa (`JwtEncoder`) ensinada na aula.

- `geraTokenComSubjectEAuthorities`: o token gerado tem o id do usuário no `sub` e os papéis na claim `authorities`.
- `jwtPropertiesRejeitaSegredoCurto`: um segredo curto demais é recusado.
- `jwtPropertiesRejeitaExpiracaoNaoPositiva`: expiração 0 é recusada.
- `jwtPropertiesRejeitaEmissorEmBranco`: emissor em branco é recusado.

**✏️ `AuthServiceTest`**

- `loginComCredenciaisValidasRetornaTokenComAuthorities`: o login usa o `AuthenticationManager` e o token sai com o papel do usuário.
- `loginComUsuarioDesativadoLancaCredenciaisInvalidasComMensagemGenerica`: um usuário desativado não entra, e a mensagem não revela o motivo.

**✏️ `AuthControllerTest`**

- `meRetornaUsuarioDoTokenAutenticado`: `GET /auth/me` devolve os dados do usuário dono do token.

**🆕 `AdminInitializerTest`**: administrador inicial criado no startup.

- `criaAdminQuandoEmailAindaNaoExiste`: na primeira subida, cria o `ADMIN` com a senha em hash BCrypt.
- `naoCriaAdminQuandoEmailJaExiste`: nas subidas seguintes, não duplica nem sobrescreve o administrador.

### Aula 14: controle de acesso / RBAC (34 execuções)

**🆕 `UsuarioFluxoIntegrationTest`**: integração completa da administração de usuários. O administrador é criado no banco e faz login real.

- `administradorCriaUsuarioComHashBcryptSemExporSenhaNaResposta`: devolve `201`; a resposta não tem senha nem hash; no banco, a senha está como hash BCrypt (`$2...`).
- `listagemAdministrativaNaoExpoeSenhaNemHash`: a listagem de usuários não mostra senha nem hash.
- `alteracaoDePapelESituacaoEhPersistidaEDesativadoNaoAutentica`: nome, papel e situação são gravados, e o usuário desativado não consegue mais logar (`401`).
- `trocaDeSenhaInvalidaASenhaAntigaEAceitaANova`: depois da troca, a senha antiga dá `401` e a nova dá `200`.
- `naoPermiteRebaixarOUltimoAdministradorAtivo`: rebaixar o único `ADMIN` devolve `422`, e ele continua `ADMIN`.
- `usuarioComumComTokenRealNaoGerenciaUsuarios`: um `USER` com JWT real em `/usuarios` recebe `403`.
- `semTokenRetorna401`: `/usuarios` sem token devolve `401`.
- `userNaoDeveGerenciarUsuarios`: exemplo literal da aula, com `@WithMockUser(roles = "USER")`, devolve `403`.
- `adminSimuladoListaUsuarios`: `@WithMockUser(roles = "ADMIN")` devolve `200`.

**🆕 `UsuarioControllerTest`**: camada web de `/usuarios`, com o serviço simulado.

- `listarComAdminRetorna200`: `ADMIN` lista usuários.
- `buscarPorIdInexistenteRetorna404`: id inexistente devolve `404`.
- `criarComAdminRetorna201`: `ADMIN` cadastra usuário.
- `atualizarComAdminRetorna200`: `ADMIN` altera nome, email, papel e situação.
- `alterarSenhaComAdminRetorna204`: `ADMIN` define uma nova senha.
- `acessoSemTokenRetorna401`: sem token devolve `401`.
- `acessoComUsuarioComumRetorna403`: com papel `USER` devolve `403`.

**🆕 `UsuarioServiceTest`**: regras de negócio da administração de usuários.

- `listarDelegaAoRepository`: a listagem vem do repositório.
- `buscarPorIdInexistenteLancaRecursoNaoEncontrado`: id inexistente lança "não encontrado".
- `criarComSucessoCodificaSenhaEUsaPapelInformado`: a senha é gravada em hash e o papel informado é respeitado.
- `criarComEmailJaCadastradoLancaExcecao`: email repetido é recusado.
- `atualizarComSucessoAlteraEmailPapelEAtivo`: email, papel e situação são atualizados.
- `atualizarAlteraONome`: o nome é atualizado. Esse teste achou um bug real: o nome era descartado. Foi o ciclo TDD 1.
- `atualizarMantendoOProprioEmailNaoLancaConflito`: manter o próprio email não conta como duplicado.
- `atualizarComEmailDeOutroUsuarioLancaConflito`: usar o email de outra pessoa é recusado.
- `rebaixarOUltimoAdministradorAtivoLancaExcecao`: não deixa rebaixar o último `ADMIN` (ciclo TDD 2).
- `desativarOUltimoAdministradorAtivoLancaExcecao`: não deixa desativar o último `ADMIN` (ciclo TDD 2).
- `rebaixarAdministradorQuandoExisteOutroAdministradorAtivoEhPermitido`: com outro `ADMIN` ativo, o rebaixamento é permitido.
- `manterOUltimoAdministradorComoAdminAtivoNaoConsultaContagem`: se nada muda no papel, o banco não é consultado à toa.
- `reativarAdministradorDesativadoNaoConsultaContagem`: reativar um administrador não precisa da contagem.
- `alterarSenhaGeraNovoHash`: a troca de senha grava um hash novo, e a senha nova confere com ele.

**✏️ `CorretoraFluxoIntegrationTest`**: autorização na escrita de corretoras.

- `cadastroSemTokenRetorna401`: cadastrar corretora sem login devolve `401`.
- `cadastroComUsuarioSemPapelAdminRetorna403`: cadastrar como `USER` devolve `403`.

**✏️ `AcaoFluxoIntegrationTest`**: autorização na escrita de ações.

- `cadastroSemTokenRetorna401`: cadastrar ação sem login devolve `401`.
- `cadastroComUsuarioSemPapelAdminRetorna403`: cadastrar como `USER` devolve `403`.

Os testes de fluxo que já existiam para corretoras e ações passaram a fazer login como `ADMIN`
antes de cadastrar, que é a regra nova.

### Aula 15: testes, TDD e cobertura (19 execuções)

**🆕 `SenhaValidacaoTest`**: `@ParameterizedTest` com valores de fronteira. São 3 métodos × 4 tamanhos, o que dá 12 execuções.

- `cadastroPeloAdministradorRespeitaLimitesDaSenha`: senhas de 7, 8, 72 e 73 caracteres em `UsuarioCreateRequest`.
- `autocadastroRespeitaLimitesDaSenha`: as mesmas fronteiras em `RegistrarUsuarioRequest`.
- `trocaDeSenhaRespeitaLimitesDaSenha`: as mesmas fronteiras em `AlterarSenhaRequest`.

Resultado esperado: 7 → inválida · 8 → válida · 72 → válida · 73 → inválida (ciclo TDD 3).

**🆕 `CorrelationIdFilterTest`**: rastreamento de requisições. São 3 métodos, que dão 4 execuções.

- `reaproveitaOCorrelationIdRecebidoEOExpoeNoMdcDuranteARequisicao`: o `X-Correlation-Id` recebido é reaproveitado e aparece nos logs.
- `geraUmUuidQuandoOCabecalhoNaoEhEnviado`: sem cabeçalho, o filtro gera um UUID.
- `geraUmUuidQuandoOCabecalhoVemEmBranco` (parametrizado): com cabeçalho vazio ou em branco, também gera um UUID.

**🆕 `GlobalExceptionHandlerTest`**: tratamento de erros.

- `excecaoInesperadaViraErro500ComMensagemGenericaSemVazarDetalhes`: um erro inesperado vira `500` sem mostrar detalhes internos.
- `excecaoDoSpringComStatusProprioPreservaOStatusEAInstanciaJaDefinida`: erros do Spring com status próprio (ex.: `405`) não viram `500`.
- `excecaoDoSpringSemInstanciaRecebeAUriDaRequisicao`: a resposta de erro informa a URI da requisição.

Também fazem parte da Aula 15, mas não são classes de teste:

- **3 ciclos TDD** (Red → Green → Refactor), com a saída real da falha: nome na atualização, último administrador e fronteiras da senha. Ver seção 5 abaixo.
- **2 testes de mutação manuais** (`<= 1` → `< 1` e `< 32` → `< 31`), os dois detectados pelos testes. Ver seção 7.
- **Portão JaCoCo** de 85% de linhas e 85% de ramos no `./mvnw clean verify`. O build falha se a cobertura cair.
- **Integração contínua**: `.github/workflows/testes.yml` roda a suíte e o portão a cada push.

### Como rodar só os testes de cada aula

```bash
# Aula 13
./mvnw test -Dtest='AuthFluxoIntegrationTest,JwtDecoderValidationTest,JwtPropertiesTest,JwtPrincipalConverterTest,JwtServiceTest,AuthServiceTest,AuthControllerTest,AdminInitializerTest'

# Aula 14
./mvnw test -Dtest='UsuarioFluxoIntegrationTest,UsuarioControllerTest,UsuarioServiceTest,CorretoraFluxoIntegrationTest,AcaoFluxoIntegrationTest'

# Aula 15
./mvnw test -Dtest='SenhaValidacaoTest,CorrelationIdFilterTest,GlobalExceptionHandlerTest'

# tudo + relatório + portão de cobertura
./mvnw clean verify
```

No Windows (PowerShell), troque `./mvnw` por `.\mvnw.cmd` e use aspas duplas no `-Dtest`.

## Complemento de testes das Aulas 13, 14 e 15 (revisão contra o material)

Esta seção registra a segunda etapa de trabalho sobre as Aulas 13, 14 e 15. A primeira etapa
(commit `ef6d0ed`) implementou login JWT, RBAC e o portão JaCoCo. Depois disso, a suíte de testes
foi comparada item por item com os arquivos das aulas no repositório
[`jeffersonarpasserini/suporteos2025`](https://github.com/jeffersonarpasserini/suporteos2025/tree/main/docs/aulas).
A comparação mostrou que os testes **cobriam quase tudo, mas não exatamente como o material pede**.
Esta etapa fechou todas as diferenças.

### 1. O que a revisão encontrou

| Aula | O que o material pede | Situação antes | Situação agora |
|---|---|---|---|
| 13 | `AuthResourceIntegrationTest`: fluxo real de login, senha errada, sem token, com token, papel sem permissão, `/auth/me` | Senha errada e `/auth/me` só testados no controller, com o serviço simulado | `AuthFluxoIntegrationTest` com contexto completo, sem mocks |
| 13 | Experimentos da seção 12.4: token adulterado e token expirado → `401` | Testados só direto no decodificador (`JwtDecoderValidationTest`), não por HTTP | Testados por requisição HTTP real |
| 13 | Testes de integração usando `@WithMockUser` | O projeto não usava a anotação em nenhum teste (e ela nem funcionava, ver seção 3) | Usada nos testes de autorização de `/usuarios` |
| 14 | `UsuarioResourceIntegrationTest` com banco, segurança e serviço reais | Só `UsuarioControllerTest`, com o serviço simulado | `UsuarioFluxoIntegrationTest` |
| 14 | Hash BCrypt gravado, senha antiga deixa de funcionar após a troca | Não verificado no banco nem de ponta a ponta | Verificado |
| 15 | Meta de 80% de ramos | 77,54% (portão em 70%) | 87,50% (portão em 85%) |
| 15 | `@ParameterizedTest` e valores de fronteira (7, 8, 72, 73) | Nenhum | `SenhaValidacaoTest`, `JwtPropertiesTest`, `CorrelationIdFilterTest` |
| 15 | Pelo menos 3 ciclos TDD com evidência de teste falhando antes | Testes escritos depois do código, sem evidência | 3 ciclos com a saída real da falha registrada |
| 15 | Teste que detecta uma alteração proposital (mutação) | Nenhum | 2 mutantes manuais, os dois detectados |
| 15 | Integração contínua (evolução sugerida) | Não existia | `.github/workflows/testes.yml` |

### 2. Testes novos

**`AuthFluxoIntegrationTest` (Aula 13).** Sobe o contexto Spring completo (H2, Liquibase, cadeia de
segurança real) e faz login de verdade antes de cada cenário.

| Teste | O que verifica | Resultado esperado |
|---|---|---|
| `loginComCredenciaisValidasEmiteJwtAssinado` | Login emite um JWT no formato `header.payload.assinatura` | `200` |
| `loginComSenhaIncorretaRetorna401` | Senha errada é recusada | `401` |
| `recursoProtegidoSemTokenRetorna401` | `/carteira/posicoes` e `/auth/me` sem cabeçalho | `401` |
| `recursoProtegidoComTokenEmitidoRetorna200` | O token emitido dá acesso a recurso protegido | `200` |
| `tokenAdulteradoRetorna401` | Um caractere da assinatura trocado | `401` |
| `tokenExpiradoRetorna401` | Token assinado com o segredo correto, mas com `exp` no passado | `401` |
| `escritaComPapelUserRetorna403` | `USER` tentando cadastrar corretora | `403` |
| `meDevolveOUsuarioDoTokenSemExporSenha` | `/auth/me` devolve id, e-mail e papel do token, sem senha nem hash | `200` |

O teste de token expirado não espera o relógio: assina um token com `exp` uma hora no passado
usando o próprio `JwtEncoder` da aplicação. Isso prova que a assinatura válida não basta, porque a
validade temporal também é conferida. Equivale ao experimento `JWT_EXPIRATION=10s` da aula.

**`UsuarioFluxoIntegrationTest` (Aula 14).** Contexto completo; o administrador é criado no banco e
faz login real antes de cada teste.

| Teste | O que verifica |
|---|---|
| `administradorCriaUsuarioComHashBcryptSemExporSenhaNaResposta` | `201`; resposta sem `senha`/`senhaHash`; no banco, o hash começa com `$2`, é diferente do texto e `matches()` confere |
| `listagemAdministrativaNaoExpoeSenhaNemHash` | A listagem traz os usuários e não contém `senha` nem nenhum hash `$2...` |
| `alteracaoDePapelESituacaoEhPersistidaEDesativadoNaoAutentica` | Nome, papel e ativo gravados no banco; usuário desativado recebe `401` no login |
| `trocaDeSenhaInvalidaASenhaAntigaEAceitaANova` | Após `PUT /usuarios/{id}/senha`: senha antiga → `401`, nova → `200` |
| `naoPermiteRebaixarOUltimoAdministradorAtivo` | Rebaixar o único `ADMIN` responde `422` e o papel continua `ADMIN` no banco |
| `usuarioComumComTokenRealNaoGerenciaUsuarios` | `USER` com JWT real em `/usuarios` → `403` |
| `semTokenRetorna401` | `/usuarios` sem token → `401` |
| `userNaoDeveGerenciarUsuarios` | Exemplo literal da aula, com `@WithMockUser(roles = "USER")` → `403` |
| `adminSimuladoListaUsuarios` | `@WithMockUser(roles = "ADMIN")` → `200` |

**Testes unitários novos (Aula 15).**

| Classe de teste | O que cobre |
|---|---|
| `SenhaValidacaoTest` | `@ParameterizedTest` com senha de 7, 8, 72 e 73 caracteres nos três contratos que recebem senha (`UsuarioCreateRequest`, `RegistrarUsuarioRequest`, `AlterarSenhaRequest`) |
| `JwtPropertiesTest` | Segredo com 31 bytes (recusado) e 32 bytes (aceito), segredo nulo, segredo medido em bytes UTF-8 (16 caracteres acentuados = 32 bytes), expiração 0 e -1 (recusadas) e 1 (aceita), emissor nulo, vazio e em branco |
| `CorrelationIdFilterTest` | Cabeçalho `X-Correlation-Id` recebido é reaproveitado e fica no MDC durante a requisição; ausente ou em branco gera um UUID; o MDC é limpo no fim |
| `GlobalExceptionHandlerTest` | Exceção inesperada vira `500` com mensagem genérica, sem vazar o texto interno; exceções do Spring com status próprio (ex.: `405`) não são mascaradas como `500` |
| `UsuarioServiceTest` (6 testes novos) | Atualização grava o nome; regra do último administrador (rebaixar, desativar, permitido quando há outro admin, sem consulta desnecessária ao banco) |

### 3. Problemas encontrados e corrigidos

1. **Nome do usuário era descartado na atualização (bug real).** `UsuarioUpdateRequest` recebia
   `nome`, mas `UsuarioService.atualizar` nunca o gravava, e a entidade `Usuario` nem tinha
   `setNome`. O teste antigo não percebia porque não verificava o nome. Corrigido pelo ciclo TDD 1.
2. **`@WithMockUser` não funcionava no projeto.** No Spring Boot 4, quem liga o Spring Security ao
   MockMvc auto-configurado é o `spring-boot-starter-security-test`. O `pom.xml` declarava só a
   biblioteca `spring-security-test`, então qualquer requisição "autenticada" pela anotação
   chegava sem usuário e virava `401`. A dependência foi trocada pelo starter.
3. **Senha acima de 72 caracteres causava erro `500`.** O BCrypt só considera os primeiros 72 bytes
   e o Spring Security atual lança exceção para senhas maiores. Agora a validação é `@Size(min = 8,
   max = 72)` nos três DTOs, e o formulário de cadastro do frontend (`registro.ts`) ganhou
   `Validators.maxLength(72)`, com a mensagem "Entre 8 e 72 caracteres".
4. **Comportamento observado, sem alteração de código:** o Spring valida o corpo (`@Valid`)
   **antes** de avaliar o `@PreAuthorize`, que só atua quando o método do controller é chamado.
   Um `USER` que envia um corpo inválido recebe `400`, não `403`. Os testes de `403` usam um corpo
   válido para isolar a regra de papel.

### 4. Regra nova: o último administrador ativo é protegido

A Aula 14 (seção 12.1) aponta que sistemas reais impedem que o último administrador seja
desativado ou rebaixado. Isso foi implementado em `UsuarioService.validarQueNaoRemoveOUltimoAdministrador`:

- se o usuário **era** `ADMIN` ativo e a alteração o deixa **não** `ADMIN` ou inativo, o serviço
  conta os administradores ativos (`UsuarioRepository.countByPapelAndAtivoTrue`);
- se houver **1 ou menos**, a alteração é recusada com `UltimoAdministradorException` → **`422`**;
- a contagem só é consultada quando a alteração realmente tira alguém da condição de admin ativo.

### 5. Ciclos TDD (Red → Green → Refactor)

Os três ciclos foram feitos com o teste escrito **antes** da implementação. Saída real do Maven:

**Ciclo 1. Atualização grava o nome.**

```text
RED    ./mvnw test -Dtest=UsuarioServiceTest
[ERROR] UsuarioServiceTest.atualizarAlteraONome <<< FAILURE!
expected: "Ana Souza"
 but was: "Nome"
[ERROR] Tests run: 9, Failures: 1, Errors: 0, Skipped: 0

GREEN  Usuario.setNome + usuario.setNome(request.nome().trim())
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
```

**Ciclo 2. Último administrador ativo.** Antes do teste foram criados só a exceção e a consulta do
repositório, sem a regra, para o teste compilar e falhar pelo motivo certo.

```text
RED
[ERROR] UsuarioServiceTest.desativarOUltimoAdministradorAtivoLancaExcecao <<< FAILURE!
Expecting code to raise a throwable.
[ERROR] UsuarioServiceTest.rebaixarOUltimoAdministradorAtivoLancaExcecao <<< FAILURE!
Expecting code to raise a throwable.
[ERROR] Tests run: 14, Failures: 2, Errors: 1, Skipped: 0

GREEN  UsuarioService.validarQueNaoRemoveOUltimoAdministrador
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
```

O erro restante na fase vermelha era o Mockito avisando que a contagem simulada nunca era
consultada, o que também era esperado, porque a regra ainda não existia. No refactor, a condição
virou um método com nome de regra de negócio e duas variáveis explicativas
(`eraAdministradorAtivo`, `continuaAdministradorAtivo`).

**Ciclo 3. Fronteiras da senha.**

```text
RED    ./mvnw test -Dtest=SenhaValidacaoTest
[ERROR] SenhaValidacaoTest.cadastroPeloAdministradorRespeitaLimitesDaSenha(int, boolean)[4] <<< FAILURE!
[ERROR] SenhaValidacaoTest.autocadastroRespeitaLimitesDaSenha(int, boolean)[4] <<< FAILURE!
[ERROR] SenhaValidacaoTest.trocaDeSenhaRespeitaLimitesDaSenha(int, boolean)[4] <<< FAILURE!
[ERROR] Tests run: 12, Failures: 3, Errors: 0, Skipped: 0

GREEN  @Size(min = 8, max = 72) nos três DTOs
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
```

Só o caso `[4]` (73 caracteres) falhou; 7, 8 e 72 já se comportavam como esperado.

### 6. Evolução da cobertura e do portão JaCoCo

| Etapa | Testes | Linhas | Ramos |
|---|---:|---:|---:|
| Linha de base (commit `ef6d0ed`) | 170 | 96,35% (818/849) | 77,54% (107/138) |
| + ciclos TDD + testes de integração das Aulas 13/14 | 205 | — | 80,26% (122/152) |
| + `JwtPropertiesTest`, `CorrelationIdFilterTest`, `GlobalExceptionHandlerTest` | 222 | 96,52% (831/861) | 84,87% (129/152) |
| + `CnpjUtilsTest`/`CepUtilsTest` (entrada nula, 1º dígito verificador errado, dígito verificador igual a 0) | **226** | **96,86% (834/861)** | **87,50% (133/152)** |

- Portão de ramos: **`0.70` → `0.80`** (meta acadêmica), elevado só depois de a suíte sustentar
  84,87%; depois **`0.80` → `0.85`**, só depois de a suíte sustentar 87,50%, deixando margem para
  flutuação. Portão de linhas mantido em `0.85`.
- Os 4 testes de `CnpjUtils`/`CepUtils` cobrem regras reais que não tinham teste: `normalizar(null)`
  devolve texto vazio, CNPJ com o 1º dígito verificador errado é rejeitado e CNPJ cujo dígito
  verificador é 0 (o da XP, `02332886000104`) é aceito.
- Os testes extras seguiram a ordem de risco da aula (seção 16): primeiro autenticação
  (`JwtProperties`), depois rastreamento de requisições e tratamento de erros.
- Nenhum limite foi reduzido, nenhuma exclusão foi adicionada ao JaCoCo e nenhum teste foi
  removido ou desativado.

Demonstração do portão (seção 14 da aula):

```text
./mvnw verify -Djacoco.minimum.branch.coverage=0.90
[INFO] Tests run: 226, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD FAILURE
[ERROR] ... jacoco-maven-plugin:0.8.12:check ... Coverage checks have not been met.

./mvnw clean verify
[INFO] Tests run: 226, Failures: 0, Errors: 0, Skipped: 0
[INFO] All coverage checks have been met.
[INFO] BUILD SUCCESS
```

Os testes passam e mesmo assim o build falha: testes aprovados não significam política de
qualidade aprovada.

### 7. Teste de mutação (alteração proposital)

Dois mutantes aplicados à mão, executados e depois revertidos:

| Mutante | Alteração | Teste que detectou | Resultado |
|---|---|---|---|
| 1 | `UsuarioService`: `countByPapelAndAtivoTrue(ADMIN) <= 1` → `< 1` | `rebaixarOUltimoAdministradorAtivoLancaExcecao`, `desativarOUltimoAdministradorAtivoLancaExcecao` | 2 falhas, mutante morto |
| 2 | `JwtProperties`: `length < 32` → `length < 31` (aceitaria chave HS256 de 248 bits) | `segredoCom31BytesEhRecusado` | 1 falha, mutante morto |

Sem os testes de fronteira (1 contra 2 administradores, 31 contra 32 bytes), os dois mutantes
sobreviveriam: a cobertura de linhas seria a mesma, mas a regra não estaria verificada.

### 8. Uso de dublês (mocks)

| Teste | Dublê | Por quê |
|---|---|---|
| `UsuarioServiceTest` | `UsuarioRepository` simulado (`@Mock`) | Isola a regra do banco. `verify(..., never()).save(...)` comprova que nada é gravado quando a regra recusa |
| `UsuarioServiceTest` | `PasswordEncoder` **real** | O que importa é "o hash confere com a senha" (`matches`); um mock só provaria que `encode` foi chamado |
| `CorrelationIdFilterTest`, `GlobalExceptionHandlerTest` | `MockHttpServletRequest`/`Response`/`MockFilterChain` | Fakes leves do spring-test, reais o bastante para observar cabeçalho e MDC sem subir servidor |
| `SenhaValidacaoTest`, `JwtPropertiesTest` | nenhum | Regras puras |
| `AuthFluxoIntegrationTest`, `UsuarioFluxoIntegrationTest` | nenhum (exceto `@WithMockUser` em 2 testes) | Integração com banco, filtros, serialização e JWT reais, como a aula recomenda |

DTOs e a própria classe testada nunca são mockados.

### 9. Integração contínua

`.github/workflows/testes.yml` roda a cada `push` e `pull_request`:

1. instala o Java 17 (Temurin) com cache do Maven;
2. executa `chmod +x mvnw` (o `mvnw` está versionado sem permissão de execução, porque o
   repositório foi criado no Windows);
3. executa `./mvnw --batch-mode clean verify`, o mesmo comando usado localmente, com testes e portão
   JaCoCo;
4. publica `target/site/jacoco/` como artefato `relatorio-jacoco`, mesmo se o build falhar.

Depois das aulas, o mesmo workflow ganhou um segundo job, **frontend (Vitest)**, que roda em paralelo
ao do backend:

1. instala o Node 22 (mesma versão do `frontend/Dockerfile`) com cache do npm;
2. executa `npm ci` e `npm run test:coverage` dentro de `frontend/` — o mesmo comando usado
   localmente, com os 165 testes e o portão de 85% do `angular.json`;
3. publica `frontend/coverage/frontend/` como artefato `relatorio-cobertura-frontend`, mesmo se falhar.

### 10. Postman

A pasta **Autenticação** da coleção ganhou **"Token adulterado (-> 401)"**. Um script de pré-requisição
pega o token do último login, troca um caractere da assinatura e chama `/auth/me`. Com isso, os
experimentos 1 a 5 da seção 12.4 da Aula 13 podem ser repetidos manualmente. O experimento 6 (token
expirado) depende de esperar a expiração, então fica coberto pelo teste automatizado.

### 11. Arquivos alterados nesta etapa

| Arquivo | Mudança |
|---|---|
| `pom.xml` | `spring-security-test` trocado por `spring-boot-starter-security-test`; portão de ramos `0.70` → `0.80` (depois `0.85`) |
| `model/Usuario.java` | `setNome` |
| `service/UsuarioService.java` | grava o nome na atualização; regra do último administrador |
| `repository/UsuarioRepository.java` | `countByPapelAndAtivoTrue(Papel)` |
| `exception/UltimoAdministradorException.java` | nova (`422`) |
| `dto/UsuarioCreateRequest.java`, `RegistrarUsuarioRequest.java`, `AlterarSenhaRequest.java` | senha entre 8 e 72 caracteres |
| `frontend/.../registro/registro.ts` e `registro.html` | `maxLength(72)` e nova mensagem de erro |
| `src/test/.../integration/AuthFluxoIntegrationTest.java` | novo |
| `src/test/.../integration/UsuarioFluxoIntegrationTest.java` | novo |
| `src/test/.../dto/SenhaValidacaoTest.java` | novo |
| `src/test/.../security/JwtPropertiesTest.java` | novo |
| `src/test/.../config/CorrelationIdFilterTest.java` | novo |
| `src/test/.../exception/GlobalExceptionHandlerTest.java` | novo |
| `src/test/.../service/UsuarioServiceTest.java` | 6 testes novos |
| `.github/workflows/testes.yml` | novo |
| `postman/gestao-acoes.postman_collection.json` | requisição de token adulterado |
| `docs/evidencias-aulas-13-14-15.md` | textos pedidos nas atividades avaliativas |

### 12. Como reproduzir

```bash
# suíte completa + relatório + portão de cobertura
./mvnw clean verify

# relatório HTML
# target/site/jacoco/index.html

# só os testes desta etapa
./mvnw test -Dtest='AuthFluxoIntegrationTest,UsuarioFluxoIntegrationTest,SenhaValidacaoTest,JwtPropertiesTest,CorrelationIdFilterTest,GlobalExceptionHandlerTest,UsuarioServiceTest'

# demonstração do portão falhando
./mvnw verify -Djacoco.minimum.branch.coverage=0.90
```

Os textos escritos que as atividades avaliativas pedem estão em
[`docs/evidencias-aulas-13-14-15.md`](docs/evidencias-aulas-13-14-15.md): por que o JWT não deve
conter dados sigilosos, a diferença entre `401` e `403`, a regra de autorização horizontal (cada
usuário só acessa a própria carteira) e onde ficou cada `@PreAuthorize` e por quê.

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
| `JWT_SECRET` | Não (tem padrão só para dev — troque em qualquer ambiente real) | Segredo de assinatura dos tokens JWT (login), mínimo 32 bytes (validado na inicialização) |
| `APP_JWT_EXPIRATION_MINUTES` | Não (padrão `120`) | Validade do token JWT |
| `APP_JWT_ISSUER` | Não (padrão `gestao-acoes`) | Emissor (`iss`) embutido no token e exigido na validação |
| `APP_ADMIN_NOME`, `APP_ADMIN_EMAIL`, `APP_ADMIN_PASSWORD` | `EMAIL`/`PASSWORD` sim (perfis `dev`/`mysql`; nunca em `test`) | Administrador inicial (Aula 14), criado no startup se o email ainda não existir — mesma filosofia de `SPRING_DATASOURCE_PASSWORD`: sem valor padrão, de propósito |
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

### Passo a passo

1. **Instale os pré-requisitos**: Docker Desktop (Windows/macOS) ou Docker Engine + plugin Compose (Linux). Confirme que estão instalados:
   ```bash
   docker --version
   docker compose version
   ```
2. **Obtenha o código** (clone o repositório ou copie a pasta do projeto) e abra um terminal na raiz — a pasta que contém `compose.yaml`.
3. **Crie o arquivo `.env`** a partir do exemplo e ajuste os valores (no mínimo `SPRING_DATASOURCE_PASSWORD` e `JWT_SECRET` — os demais têm padrão funcional):
   ```powershell
   Copy-Item .env.example .env
   ```
   ```bash
   # Linux/macOS
   cp .env.example .env
   ```
4. **Suba os três serviços** (PostgreSQL, backend Spring Boot e frontend Angular/Nginx), construindo as imagens na primeira vez:
   ```bash
   docker compose up -d --build
   ```
   Na primeira execução isso baixa as imagens base e builda o Maven e o Angular dentro do container — pode levar alguns minutos. Execuções seguintes reaproveitam cache e são bem mais rápidas.
5. **Acompanhe a inicialização** até a aplicação ficar saudável (o frontend só sobe depois que o backend responde no `healthcheck`):
   ```bash
   docker compose logs -f aplicacao
   ```
6. **Acesse `http://localhost:8080`** no navegador. Frontend e API já estão no ar na mesma porta (Swagger em `http://localhost:8080/swagger-ui.html`) — nenhum outro passo é necessário para uso local.

Com `APP_PORT=8080` (padrão), o frontend Angular e a API ficam disponíveis na mesma porta. O container `aplicacao` não publica porta própria no host — o Nginx do container `frontend` faz proxy reverso das rotas da API (`/auth`, `/acoes`, `/carteira`, `/corretoras`, `/proventos`, `/usuarios`, `/actuator`, `/swagger-ui`, `/v3/api-docs`) para ele pela rede interna do Compose. Se `POSTGRES_PORT`/`APP_PORT` estiverem ocupadas, altere-as no `.env` — a comunicação interna entre os containers continua em `postgres:5432` e `aplicacao:8080`.

### Liberando o acesso para outras pessoas na mesma rede

Por padrão o Docker publica a porta em todas as interfaces de rede da máquina host (não só `localhost`), então qualquer pessoa na mesma rede local (Wi-Fi/LAN) já consegue acessar sem configuração extra no projeto:

1. **Descubra o IP local da máquina que está rodando os containers**:
   ```powershell
   ipconfig    # Windows — veja "Endereço IPv4" do adaptador em uso (ex.: 192.168.0.42)
   ```
   ```bash
   ip addr     # Linux
   ifconfig    # macOS
   ```
2. **Libere a porta no firewall da máquina host**, se necessário. No Windows, ao subir o Docker Desktop pela primeira vez o Firewall do Windows costuma perguntar se libera o acesso em redes privadas — aceite. Se a porta continuar bloqueada, libere manualmente a porta `8080/TCP` (ou o valor de `APP_PORT`) para redes privadas no Firewall do Windows Defender.
3. **Compartilhe o endereço** `http://<IP-da-máquina>:8080` com a outra pessoa — ela acessa pelo navegador dela, na mesma rede. Não é preciso mexer em CORS: em produção o frontend chama a API com URL relativa (mesma origem, mesmo host/porta — ver `frontend/src/environments/environment.ts`), então funciona automaticamente por qualquer IP/hostname usado para acessar.

> Isso cobre acesso dentro da mesma rede local. Para expor a aplicação pela internet (fora da rede local) seria necessário port forwarding no roteador ou um serviço de túnel, além de reforços de segurança que fogem do escopo deste projeto acadêmico — não recomendado sem isso.

### Operação do dia a dia

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

A suíte completa (226 testes na versão atual: unitários, `@DataJpaTest`, `@WebMvcTest` e integração ponta a ponta) roda inteiramente sobre H2 e mocks HTTP locais (`MockWebServer`) — **nenhum teste faz chamada de rede real** às APIs externas, portanto a suíte não consome cotas de nenhum provedor. As fatias `@WebMvcTest` de `/carteira`, `/proventos`, `/usuarios` e de `/auth` (só o teste de `/auth/me`) carregam a `SecurityFilterChain` real (`SecurityConfig` + `JwtPrincipalConverter`) para exercitar `401`/`403` de verdade; as demais (`/corretoras`, `/acoes`) desligam os filtros de servlet (`addFilters = false`) por testarem só a lógica do controller — a autorização desses dois endpoints é coberta pelos testes de integração ponta a ponta (`CorretoraFluxoIntegrationTest`/`AcaoFluxoIntegrationTest`), com contexto completo.

```bash
./mvnw clean verify
```

Além dos testes, `verify` gera o relatório de cobertura JaCoCo (`target/site/jacoco/index.html`) e falha o build se a cobertura cair abaixo do portão configurado em `pom.xml` (`jacoco.minimum.line.coverage`/`jacoco.minimum.branch.coverage`) — ver detalhes na seção [Aderência às aulas de login JWT, controle de acesso e testes/cobertura](#aderência-às-aulas-de-login-jwt-controle-de-acesso-e-testescobertura).

Testes do frontend (165 testes, portão de 85% de cobertura — ver [`frontend/README.md`](frontend/README.md)):

```bash
cd frontend
npm run test:coverage   # relatório em frontend/coverage/frontend/index.html
```

**Nota de ambiente**: o perfil de testes é fixado por `src/test/resources/application.properties` (`spring.profiles.active=test`). Se os testes começarem a conectar em um PostgreSQL local em vez do H2 em memória, confira esse arquivo — algum editor pode corrompê-lo (ex.: autocomplete inserindo uma quebra de linha no meio de `spring.profiles.active`).

## Endpoints principais

| Método | Caminho | Autenticação | Descrição |
|---|---|---|---|
| `POST` | `/corretoras` | **Sim** (ADMIN) | Cadastra corretora (CNPJ validado + enriquecido via BrasilAPI, validado na CVM, endereço via ViaCEP) |
| `GET` | `/corretoras` | Não | Lista corretoras (paginado) |
| `GET` | `/corretoras/{id}` | Não | Busca corretora por ID |
| `GET` | `/corretoras/cnpj/{cnpj}` | Não | Busca corretora por CNPJ (com ou sem máscara) |
| `POST` | `/acoes` | **Sim** (ADMIN) | Cadastra ação (ticker validado e cotação obtida do provedor do mercado) |
| `GET` | `/acoes` | Não | Lista ações (paginado) |
| `GET` | `/acoes/{id}` | Não | Busca ação por ID |
| `GET` | `/acoes/ticker/{ticker}` | Não | Busca ação por ticker (globalmente único — RN07) |
| `PUT` | `/acoes/{id}/atualizar-cotacao` | **Sim** (ADMIN) | Atualiza a cotação de uma ação já cadastrada |
| `POST` | `/auth/registrar` | Não | Cadastra usuário (nome, email, senha) — sempre com papel `USER` |
| `POST` | `/auth/login` | Não | Autentica e retorna um token JWT (inclui as authorities do papel do usuário) |
| `GET` | `/auth/me` | **Sim** | Devolve o usuário representado pelo token (nome, email, papel, situação) |
| `GET` | `/usuarios` | **Sim** (ADMIN) | Lista usuários cadastrados (paginado) |
| `GET` | `/usuarios/{id}` | **Sim** (ADMIN) | Busca usuário por ID |
| `POST` | `/usuarios` | **Sim** (ADMIN) | Cadastra usuário definindo o papel explicitamente (`ADMIN` ou `USER`) |
| `PUT` | `/usuarios/{id}` | **Sim** (ADMIN) | Altera nome, email, papel e situação (`ativo`) de um usuário |
| `PUT` | `/usuarios/{id}/senha` | **Sim** (ADMIN) | Define uma nova senha para o usuário |
| `POST` | `/carteira/lancamentos` | **Sim** (Bearer JWT) | Registra uma compra ou venda (tipo, ação, corretora, quantidade, preço, data); venda acima do saldo disponível retorna `422` |
| `GET` | `/carteira/lancamentos` | **Sim** | Lista os lançamentos do usuário autenticado (paginado) |
| `DELETE` | `/carteira/lancamentos/{id}` | **Sim** | Exclui um lançamento do próprio usuário (recalcula a posição do ativo) |
| `GET` | `/carteira/posicoes` | **Sim** | Posições consolidadas por ativo, por custo médio ponderado (quantidade líquida, preço médio, valor investido/atual, variação, resultado realizado em vendas) |
| `POST` | `/proventos` | **Sim** | Registra um provento (dividendo/JCP) recebido |
| `GET` | `/proventos` | **Sim** | Lista os proventos do usuário autenticado (paginado, mais recente primeiro) |

Consultar corretoras e ações continua público (sem login), preservando RF01-RF12 do MVP original. Cadastrar/atualizar corretoras e ações agora exige autenticação **e** papel `ADMIN` (Aula 14 — RBAC): sem token é `401`; autenticado mas sem `ADMIN` é `403`. Carteira e proventos continuam exigindo apenas um usuário autenticado (qualquer papel), já que cada um opera só a própria carteira — isso nunca dependeu de ser `ADMIN`. Ver a decisão original em `openspec/changes/adicionar-carteira-auth-frontend-angular/design.md` e a extensão de RBAC na seção [Aderência às aulas de segurança e testes](#aderência-às-aulas-de-login-jwt-controle-de-acesso-e-testescobertura) abaixo.

Payloads, exemplos de sucesso/erro e todos os códigos HTTP estão documentados no Swagger UI (botão "Authorize" para informar o token JWT) e nas specs em `openspec/specs/` (baseline atual; as mudanças que as originaram estão arquivadas em `openspec/changes/archive/`). Uma coleção Postman com exemplos prontos está em `postman/gestao-acoes.postman_collection.json`.

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

Fora de escopo (ver `proposal.md`/`design.md` de `openspec/changes/archive/*/`, seção "Non-Goals"): edição de lançamentos já registrados (exclusão já é suportada — ver `DELETE /carteira/lancamentos/{id}` acima), apuração de IR (DARF, isenção mensal, day-trade), refresh token/OAuth2/login social, revogação de token antes da expiração (desativar um usuário bloqueia logins futuros, mas um token já emitido continua válido até expirar — ver Aula 14 na seção de aderência), histórico de cotações, cache avançado, circuit breaker, fallback automático entre provedores US, métricas/tracing, filtros de busca além de paginação simples. Autorização por papéis (RBAC) **deixou de ser uma limitação**: implementada na Aula 14 (ver seção de aderência), com papéis `ADMIN`/`USER`.

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
  config/         segurança (Spring Security, RBAC), bootstrap do admin inicial e configuração transversal
  security/       JWT (emissão via JwtEncoder, UserDetailsService, conversor de authorities, handlers de erro)
  integration/    portas + adaptadores por provedor externo (Ports and Adapters)

frontend/         aplicação Angular (interface do sistema) — ver frontend/README.md
```