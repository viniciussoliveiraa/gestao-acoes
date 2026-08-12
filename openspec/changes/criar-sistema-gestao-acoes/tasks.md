## 1. Bootstrap do repositório, Maven e OpenSpec

- [x] 1.1 Confirmar `pom.xml` com Java 17, versão estável do Spring Boot fixada, e dependências: Spring Web, Spring Data JPA, Bean Validation, spring-cloud-starter-openfeign (via BOM `spring-cloud-dependencies:2025.1.2`), springdoc-openapi-starter-webmvc-ui:3.1.0, driver H2, driver PostgreSQL, Flyway, JUnit 5, Mockito, spring-boot-starter-test, WireMock 3.13.2. Testcontainers descartado (ver nota em `design.md`); testes de integração usam H2. Verificação: `./mvnw -q dependency:resolve` sem erro (confirmado).
- [x] 1.2 Estruturar pacotes: `controller`, `service`, `repository`, `model`, `dto`, `mapper`, `exception`, `config`, `integration/{cnpj,cep,instituicao,cotacao}` (criados conforme as classes de cada fase são adicionadas). Verificação: `./mvnw -q compile` passa.
- [x] 1.3 Habilitar `@EnableFeignClients` na classe principal. Verificação: contexto sobe em teste `contextLoads()` (confirmado, ver 2.3).

## 2. Configuração, perfis e migrations

- [x] 2.1 Criar `application.yml` base com placeholders de `BRASIL_API_BASE_URL`, `VIACEP_BASE_URL`, `BRAPI_BASE_URL`, `BRAPI_TOKEN`, `TWELVE_DATA_BASE_URL`, `TWELVE_DATA_API_KEY`, `ALPHA_VANTAGE_BASE_URL`, `ALPHA_VANTAGE_API_KEY`, `app.market-data.us-provider=twelve-data`. Verificação: nenhum valor secreto real no arquivo versionado (confirmado, só defaults públicos).
- [x] 2.2 Criar perfil `test` (H2, ativado via `src/test/resources/application.properties`) e perfil `dev`/default (PostgreSQL, `ddl-auto=validate`). Verificação: `./mvnw test` usa H2 (confirmado no log: "The following 1 profile is active: test").
- [x] 2.3 Criar migrations Flyway `V1__create_corretora.sql` e `V2__create_acao.sql` com as constraints de `design.md`. Verificação: `GestaoAcoesApplicationTests#contextLoads` passa e log confirma "Successfully applied 2 migrations to schema PUBLIC, now at version v2".
- [x] 2.4 Criar `.env.example` com todas as variáveis de 2.1, sem valores reais. Verificação: revisão manual, nenhum segredo.

## 3. Domínio, DTOs, mappers e validações

- [x] 3.1 Criar entidades JPA `Corretora` e `Acao` (sem Lombok/`@Data`). Verificação: compila e mapeia para as tabelas das migrations (confirmado via `contextLoads`).
- [x] 3.2 Criar enums `Mercado` (`BRASIL`, `ESTADOS_UNIDOS`) e `Moeda` (`BRL`, `USD`). Verificação: usados nas entidades e DTOs.
- [x] 3.3 Criar DTOs (records) de entrada (`CorretoraRequest`, `AcaoRequest`) com Bean Validation e de saída (`CorretoraResponse`, `AcaoResponse`); paginação usa `Page<Response>` do Spring Data. Verificação: entidades JPA nunca retornadas por controller (a confirmar na Fase 7).
- [x] 3.4 Criar `CnpjUtils` (normalização + dígitos verificadores), `CepUtils` (normalização + formato 8 dígitos) e `TickerUtils` (normalização). Verificação: `CnpjUtilsTest`, `CepUtilsTest`, `TickerUtilsTest` verdes (12 testes).
- [x] 3.5 Criar mappers (`CorretoraMapper`, `AcaoMapper`) entidade→DTO de resposta. Verificação: `CorretoraMapperTest`, `AcaoMapperTest` verdes.

## 4. Persistência e constraints

- [x] 4.1 Criar `CorretoraRepository` e `AcaoRepository` (Spring Data JPA) com `findByCnpj`, `findByTicker` (ticker globalmente único, RN07 — ver revisão em `design.md`); paginação herdada de `JpaRepository`. Verificação: `@DataJpaTest` cobrindo busca por CNPJ e por `ticker` (verde).
- [x] 4.2 Testar violação das constraints únicas via `@DataJpaTest` (corretora duplicada, RN07 ação duplicada) e confirmar que o mesmo ticker é rejeitado mesmo em mercados diferentes. Verificação: `CorretoraRepositoryTest` (2 testes) e `AcaoRepositoryTest` (3 testes) verdes. Nota: pacotes de `@DataJpaTest`/`@AutoConfigureTestDatabase` mudaram no Spring Boot 4 para `org.springframework.boot.data.jpa.test.autoconfigure` e `org.springframework.boot.jdbc.test.autoconfigure`, respectivamente.

## 5. Portas e adaptadores externos

- [x] 5.1 Confirmar contratos reais via chamada HTTP direta (curl) durante a especificação: BrasilAPI CNPJ (`/api/cnpj/v1/{cnpj}`), BrasilAPI/CVM (`/api/cvm/corretoras/v1/{cnpj}` — existe consulta direta por CNPJ, não só lista), ViaCEP, brapi.dev (`/api/v2/stocks/quote`), Twelve Data (`/quote`), Alpha Vantage (`GLOBAL_QUOTE`). Verificação: tabela de contratos em `design.md` atualizada com campos e comportamentos reais confirmados.
- [x] 5.2 Criar `CnpjDataPort` + `BrasilApiCnpjAdapter` (Feign), distinguindo 404 (não encontrado) de erro de infraestrutura via `catch (FeignException.NotFound)`/`catch (RuntimeException)`. Verificação: `BrasilApiCnpjAdapterTest` (3 testes, MockWebServer) cobrindo sucesso, 404, 500.
- [x] 5.3 Criar `EnderecoPort` + `ViaCepAdapter`, distinguindo CEP malformado (validado localmente antes da chamada, via `CepUtils`) de CEP inexistente (`erro:"true"`). Verificação: `ViaCepAdapterTest` (3 testes) verde.
- [x] 5.4 Criar `InstituicaoFinanceiraPort` + `BrasilApiCvmAdapter`. Sem necessidade de cache/TTL — existe endpoint de consulta direta por CNPJ na BrasilAPI. Verificação: `BrasilApiCvmAdapterTest` (4 testes: ativa, cancelada, não encontrada, falha de infraestrutura) verde.
- [x] 5.5 Criar `CotacaoPort` + `BrapiAdapter` (mercado `BRASIL`). Verificação: `BrapiAdapterTest` (3 testes: sucesso, resultado vazio → ticker não encontrado, token ausente → falha de integração) verde.
- [x] 5.6 Criar `TwelveDataAdapter`, `AlphaVantageAdapter` (mercado `ESTADOS_UNIDOS`) e `CotacaoStrategyResolver` selecionando por `app.market-data.us-provider`. Verificação: `TwelveDataAdapterTest` (3), `AlphaVantageAdapterTest` (3) e `CotacaoStrategyResolverTest` (3, confirma que só o adaptador configurado é resolvido) verdes.
- [x] 5.7 Configurar timeouts de conexão/leitura globais via `feign.client.config.default` em `application.yml` (3s/5s). Verificação: propriedade presente e aplicada a todos os `@FeignClient` (nenhum client sobrescreve `connectTimeout`/`readTimeout`).

## 6. Serviços e regras de negócio

- [x] 6.1 Implementar `CorretoraService.registrar` orquestrando CNPJ → duplicidade → CEP (formato) → CVM → endereço → persistência (sem `@Transactional` explícito: chamadas externas ficam fora de transação e o único `save()` já é atômico via Spring Data), sem persistência parcial (RN10). Verificação: `CorretoraServiceTest` (6 testes: sucesso, CNPJ inválido, CNPJ duplicado, CEP inválido, instituição não validada, falha externa não persiste).
- [x] 6.2 Implementar `AcaoService.registrar` orquestrando normalização → duplicidade → seleção de estratégia → validação de ticker → persistência (RN05-RN07, RN10). Verificação: testes cobrindo ticker válido, ticker inexistente, duplicidade (parte de `AcaoServiceTest`).
- [x] 6.3 Implementar `AcaoService.atualizarCotacao` mantendo a última cotação válida em caso de falha do provedor (RN11) — a entidade só é mutada/salva após a chamada externa ter sucesso. Verificação: teste `falhaNaAtualizacaoMantemUltimaCotacaoValida` confirma que o valor não muda e `save` nunca é chamado.
- [x] 6.4 Implementar paginação (`listar` via `Page<T>` do Spring Data) e buscas (`buscarPorId`, `buscarPorCnpj`, `buscarPorTicker` — sem parâmetro de mercado, já que o ticker é globalmente único). Verificação: `AcaoServiceTest#buscarPorTickerInexistenteLancaRecursoNaoEncontrado`/`#buscarPorTickerComSucessoNormalizaEntrada` verdes.

## 7. Controllers e contratos REST

- [x] 7.1 Implementar `CorretoraController`: `POST /corretoras`, `GET /corretoras`, `GET /corretoras/{id}`, `GET /corretoras/cnpj/{*cnpj}` (catch-all path variable — CNPJ mascarado contém `/`, que precisa capturar o restante do path; ver nota em `design.md`). Verificação: `@WebMvcTest` em 8.1 (após o handler global existir), validado também manualmente com a aplicação real rodando.
- [x] 7.2 Implementar `AcaoController`: `POST /acoes`, `GET /acoes`, `GET /acoes/{id}`, `GET /acoes/ticker/{ticker}`, `PUT /acoes/{id}/atualizar-cotacao`. Verificação: `@WebMvcTest` em 8.1.
- [x] 7.3 Garantir que nenhum controller chama adaptador de integração diretamente (apenas via service). Verificação: revisão de código — `CorretoraController`/`AcaoController` só importam `service`/`mapper`/`dto`/`model`, nenhuma dependência de `integration.*`.

## 8. Tratamento global de erros

- [x] 8.1 Implementar `GlobalExceptionHandler` (`@RestControllerAdvice`) com `ProblemDetail` (RFC 9457) para `ApiException` (status por subtipo, ver tabela em `design.md`), `MethodArgumentNotValidException` (400, com lista de campos), `DataIntegrityViolationException` (409) e fallback genérico (500 apenas para erro de fato inesperado; exceções internas do Spring MVC que implementam `ErrorResponse`, como `NoResourceFoundException`, reaproveitam seu próprio `ProblemDetail` — bug encontrado em teste manual e corrigido, ver nota em `design.md`). Verificação: `CorretoraControllerTest` (10 testes, incluindo `rotaInexistenteRetorna404` e `buscarPorCnpjComMascaraContendoBarraFuncionaNaUrl`) e `AcaoControllerTest` (9 testes).
- [x] 8.2 Adicionar `CorrelationIdFilter` (`OncePerRequestFilter`) gerando/propagando `X-Correlation-Id` via MDC, consumido pelo padrão de log em `application.yml` (`logging.pattern.correlation`). Nenhum adaptador loga CNPJ completo ou chave de API (mensagens de exceção usam apenas o ticker/CNPJ mascarado ou nenhum dado sensível — ver `BrasilApiCnpjAdapter.mascarar`). Verificação: revisão de código; nenhuma chamada a `log`/`System.out` com `token`, `apiKey` ou CNPJ completo.

## 9. Testes unitários, de contrato e integração

- [x] 9.1 Cobertura unitária de RN01-RN11: normalização/validação de CNPJ e CEP (`CnpjUtilsTest`, `CepUtilsTest`), unicidade (RN07, `AcaoRepositoryTest`/`CorretoraRepositoryTest`), rejeição de instituição não validada (RN03, `CorretoraServiceTest`), precisão monetária `BigDecimal` escala 4 (RN08, todos os adaptadores de cotação), timezone preservado via `OffsetDateTime` (RN09), nenhuma persistência parcial (RN10, `CorretoraServiceTest#falhaExternaAposEtapasAnterioresNaoPersisteNada`), última cotação mantida em falha (RN11, `AcaoServiceTest#falhaNaAtualizacaoMantemUltimaCotacaoValida`). Verificação: `./mvnw test` verde (74 testes).
- [x] 9.2 Criar testes de integração ponta a ponta com H2 e adaptadores mockados via MockWebServer: `CorretoraFluxoIntegrationTest` (`POST /corretoras` + `GET /corretoras/{id}`) e `AcaoFluxoIntegrationTest` (`POST /acoes` + `PUT /acoes/{id}/atualizar-cotacao`). Verificação: 2 testes verdes, suíte inteira sem qualquer chamada de rede real (todas as URLs-base de integração são sobrescritas para `localhost` nos testes).
- [x] 9.3 Confirmar teste de contexto (`contextLoads`) e execução completa da suíte em perfil `test`. Verificação: `./mvnw test` executado 2x consecutivas, ambas com 74 testes, 0 falhas, 0 erros.

## 10. Documentação OpenAPI, README e coleção de requisições

- [x] 10.1 springdoc-openapi já estava nas dependências (Fase 1); controllers usam DTOs tipados que o springdoc documenta automaticamente. Verificação: aplicação subida localmente (perfil `test`, porta 8080) — `GET /v3/api-docs` → `200` com todos os paths (`/corretoras*`, `/acoes*`); `GET /swagger-ui.html` → `302` (redirect padrão para `/swagger-ui/index.html`).
- [x] 10.2 Escrever `README.md` completo (pré-requisitos, IntelliJ, variáveis de ambiente, perfis, execução, testes, endpoints, limitações de cada API externa com data de verificação 2026-08-12). Verificação: conteúdo revisado manualmente; comandos de execução/teste conferem com o `pom.xml`/perfis reais.
- [x] 10.3 Criar coleção Postman (`postman/gestao-acoes.postman_collection.json`) com exemplos de sucesso e erro para os 9 endpoints. Verificação: JSON validado sintaticamente (`JSON.parse` sem erro).
- [x] 10.4 Criar diagrama simplificado de entidades e diagrama de componentes em `docs/diagramas.md` (Mermaid), reaproveitando o diagrama de componentes de `design.md`. Verificação: arquivo criado com os dois diagramas.

## 11. Verificação final, rastreabilidade e apresentação

- [x] 11.1 Revisar a matriz de rastreabilidade (`traceability.md`), atualizada com os nomes reais das classes de teste (26 linhas, cada uma com pelo menos um teste). Verificação: nenhuma linha sem teste correspondente.
- [x] 11.2 Rodar `./mvnw verify` (build + testes) e `openspec validate criar-sistema-gestao-acoes --strict`. Verificação: `./mvnw verify` → exit 0 (BUILD SUCCESS, 74 testes); `openspec validate --strict` → "Change 'criar-sistema-gestao-acoes' is valid".
- [x] 11.3 Checklist de "pronto": build compilando ✔, 74/74 testes passando em 2 execuções consecutivas ✔, OpenAPI acessível (`/v3/api-docs` 200, verificado com app rodando) ✔, migrations aplicáveis em banco limpo (Flyway aplica V1+V2 do zero em cada execução de teste) ✔, nenhuma credencial versionada (`git status` revisado, só `.env.example` com placeholders) ✔, README reproduzível ✔.
- [x] 11.4 Roteiro de apresentação prática criado em `docs/roteiro-apresentacao.md` (6 seções: corretora sucesso/erro, ação sucesso/erro BR+US, atualização de cotação com falha, documentação/testes).