# Sistema de Gestão de Ações

API REST acadêmica para cadastro e consulta de corretoras e ações, com dados validados/enriquecidos por integrações externas reais (CNPJ, CEP, cotações BR/US). Desenvolvida com Specification-Driven Development via [OpenSpec](https://github.com/Fission-AI/OpenSpec) — a especificação completa (proposta, specs por capacidade, design técnico e plano de tarefas) está em `openspec/changes/criar-sistema-gestao-acoes/`.

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

O perfil ativo é controlado por `SPRING_PROFILES_ACTIVE`. O schema é criado/versionado por migrations Flyway — `dev`/`test` usam `src/main/resources/db/migration/postgresql` (H2 no perfil `test` roda em `MODE=PostgreSQL`, compatível com essa sintaxe); `mysql` usa `src/main/resources/db/migration/mysql`, com sintaxe própria (`AUTO_INCREMENT`, `DECIMAL`, `TIMESTAMP(6)`, `ENGINE=InnoDB`). Nenhum perfil usa `ddl-auto=create`/`update`.

Para rodar com MySQL:
```powershell
# Crie o banco e o usuário primeiro (num cliente MySQL):
# CREATE DATABASE gestao_acoes;
# CREATE USER 'gestao_acoes'@'localhost' IDENTIFIED BY 'gestao_acoes';
# GRANT ALL PRIVILEGES ON gestao_acoes.* TO 'gestao_acoes'@'localhost';

$env:SPRING_PROFILES_ACTIVE = "mysql"
./mvnw spring-boot:run
```

**Nota de honestidade**: o perfil `mysql` não foi validado contra um servidor MySQL real neste ambiente de desenvolvimento (não havia um disponível). A sintaxe das migrations e o mapeamento das entidades foram validados via `MysqlMigrationSmokeTest`, que roda H2 em modo de compatibilidade MySQL (`MODE=MySQL`) — uma aproximação razoável, mas não uma garantia absoluta de compatibilidade 100% com MySQL real. Se encontrar algum erro de schema ao rodar contra MySQL de verdade, é o primeiro lugar a investigar.

## Variáveis de ambiente

Ver `.env.example` para a lista completa. Resumo:

| Variável | Obrigatória | Descrição |
|---|---|---|
| `SPRING_DATASOURCE_URL`, `_USERNAME`, `_PASSWORD` | Sim (perfil `dev`) | Conexão PostgreSQL |
| `APP_MARKET_DATA_US_PROVIDER` | Não (padrão `twelve-data`) | `twelve-data` ou `alpha-vantage` |
| `BRASIL_API_BASE_URL` | Não (tem padrão público) | Dados de CNPJ e validação CVM |
| `VIACEP_BASE_URL` | Não (tem padrão público) | Consulta de CEP |
| `BRAPI_BASE_URL`, `BRAPI_TOKEN` | Token opcional para os tickers `PETR4`, `MGLU3`, `VALE3`, `ITUB4`; obrigatório para os demais | Cotações do mercado brasileiro |
| `TWELVE_DATA_BASE_URL`, `TWELVE_DATA_API_KEY` | Chave obrigatória se `us-provider=twelve-data` | Cotações do mercado americano |
| `ALPHA_VANTAGE_BASE_URL`, `ALPHA_VANTAGE_API_KEY` | Chave obrigatória se `us-provider=alpha-vantage` | Cotações do mercado americano (alternativa) |

Nenhuma chave real está versionada no repositório; `.env.example` contém apenas placeholders.

## Executando a aplicação

```bash
# Perfil dev (requer PostgreSQL local acessível pelas variáveis acima)
./mvnw spring-boot:run

# Sem PostgreSQL disponível, suba com H2 apenas para exploração manual
./mvnw spring-boot:run -Dspring-boot.run.profiles=test
```

Após subir, a documentação interativa fica disponível em `http://localhost:8080/swagger-ui.html` e o OpenAPI JSON em `http://localhost:8080/v3/api-docs`.

## Executando os testes

```bash
./mvnw test
```

A suíte completa (74 testes na versão atual: unitários, `@DataJpaTest`, `@WebMvcTest` e integração ponta a ponta) roda inteiramente sobre H2 e mocks HTTP locais (`MockWebServer`) — **nenhum teste faz chamada de rede real** às APIs externas, portanto a suíte não consome cotas de nenhum provedor.

## Endpoints principais

| Método | Caminho | Descrição |
|---|---|---|
| `POST` | `/corretoras` | Cadastra corretora (CNPJ validado + enriquecido via BrasilAPI, validado na CVM, endereço via ViaCEP) |
| `GET` | `/corretoras` | Lista corretoras (paginado) |
| `GET` | `/corretoras/{id}` | Busca corretora por ID |
| `GET` | `/corretoras/cnpj/{cnpj}` | Busca corretora por CNPJ (com ou sem máscara) |
| `POST` | `/acoes` | Cadastra ação (ticker validado e cotação obtida do provedor do mercado) |
| `GET` | `/acoes` | Lista ações (paginado) |
| `GET` | `/acoes/{id}` | Busca ação por ID |
| `GET` | `/acoes/ticker/{ticker}` | Busca ação por ticker (globalmente único — RN07) |
| `PUT` | `/acoes/{id}/atualizar-cotacao` | Atualiza a cotação de uma ação já cadastrada |

Payloads, exemplos de sucesso/erro e todos os códigos HTTP estão documentados no Swagger UI e na spec `openspec/changes/criar-sistema-gestao-acoes/specs/gestao-corretoras` / `gestao-acoes`. Uma coleção Postman com exemplos prontos está em `postman/gestao-acoes.postman_collection.json`.

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

Fora de escopo nesta versão (ver `proposal.md` da mudança OpenSpec, seção "diferenciais"): histórico de cotações, entidade `Carteira`, autenticação/autorização, cache avançado, circuit breaker, fallback automático entre provedores US, métricas/tracing, filtros de busca além de paginação simples.

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
  config/         filtros e configuração transversal
  integration/    portas + adaptadores por provedor externo (Ports and Adapters)
```