# Roteiro de apresentação prática

Pré-requisito: aplicação rodando (`./mvnw spring-boot:run`, perfil `dev` com PostgreSQL ou perfil `test` com H2 para uma demonstração rápida sem infraestrutura). Swagger UI em `http://localhost:8080/swagger-ui.html`.

## 0. Provas de aderência às aulas de apoio (variáveis de ambiente, Liquibase, Docker)

Mostrar isto primeiro, antes da demonstração funcional — é o mais rápido de provar.

### Prova mais direta: as tags do Git

Cada aula pede, no "Ponto de versionamento", para marcar o commit com `git tag`. O projeto tem as três tags, na ordem correta:

```bash
git tag -l
# aula-10-variaveis-ambiente
# aula-11-liquibase
# aula-12-docker

git log -1 --format="%h %ad %s" --date=short aula-10-variaveis-ambiente
git log -1 --format="%h %ad %s" --date=short aula-11-liquibase
git log -1 --format="%h %ad %s" --date=short aula-12-docker
```

Também dá pra abrir direto no GitHub: `https://github.com/viniciussoliveiraa/gestao-acoes/releases/tag/aula-11-liquibase` (troque o nome da tag).

### Aula 10 — Variáveis de ambiente

```bash
cat .env.example                # variaveis documentadas, sem segredo real
git check-ignore -v .env        # confirma que o .env local (com senha real) é ignorado
git log --all --full-history -- .env    # nao retorna nada: nunca foi commitado
```

Mostrar também `src/main/resources/application.yml`, linha `password: ${SPRING_DATASOURCE_PASSWORD}` — sem valor padrão, a aplicação recusa subir sem a variável.

### Aula 11 — Liquibase

```bash
ls src/main/resources/db/changelog/changes     # changesets numerados 001 a 006
cat src/main/resources/db/changelog/db.changelog-master.xml   # include de cada changeset
```

Rodar a aplicação e mostrar no log o Liquibase executando (ou reconhecendo) os changeSets:

```bash
./mvnw spring-boot:run
```

Procurar no console por linhas `liquibase.changelog : ChangeSet ... ran successfully` na primeira execução, e por `Liquibase: Update has been successful. Rows affected: 0` (nada a fazer) numa segunda execução — prova que o histórico é respeitado. Mostrar também `spring.jpa.hibernate.ddl-auto=validate` no `application.yml` — o Hibernate não cria mais tabela nenhuma.

### Aula 12 — Docker e Docker Compose

```bash
docker compose up -d --build
docker compose ps                     # postgres "healthy", aplicacao/frontend "running"
docker compose logs aplicacao | grep -i liquibase   # liquibase rodando dentro do container
```

Acessar `http://localhost:8080` (frontend) e `http://localhost:8080/swagger-ui.html` (API) para provar que subiu de fato.

```bash
docker compose down                   # para os containers, preserva o volume postgres_data
docker compose up -d                  # sobe de novo: dados continuam la
```

## 1. Cadastro de corretora — caminho feliz

`POST /corretoras` com um CNPJ real de corretora ativa (ex.: `44527444000155`, ABN AMRO Clearing) e um CEP válido (ex.: `01310100`).

- Mostrar que a resposta (`201`) traz `razaoSocial`, `situacaoCadastral` e endereço **vindos das APIs externas**, não do payload enviado.
- Repetir a mesma chamada → `409` (CNPJ duplicado).

## 2. Cadastro de corretora — cenários de erro

- CNPJ com dígito verificador errado → `400`, sem nenhuma chamada externa (mostrar nos logs/Swagger que a resposta é imediata).
- CNPJ de uma empresa que existe mas não é corretora (qualquer CNPJ comum) → `422` (RN03: instituição não validada na CVM).
- CEP inexistente com formato válido (ex.: `99999999`) → `404`.

## 3. Cadastro de ação — caminho feliz (BR e US)

- `POST /acoes` com `{"ticker":"PETR4","mercado":"BRASIL"}` → `201`, cotação real da brapi.dev.
- `POST /acoes` com `{"ticker":"AAPL","mercado":"ESTADOS_UNIDOS"}` (requer `TWELVE_DATA_API_KEY` ou `ALPHA_VANTAGE_API_KEY` configurada) → `201`.
- Mostrar `GET /acoes/ticker/PETR4` (ticker é globalmente único — RN07 — não precisa de `mercado` na busca).

## 4. Cadastro de ação — cenários de erro

- Ticker inexistente (`ZZZZ9`) → `404`.
- Repetir `PETR4`/`BRASIL` já cadastrado → `409`.

## 5. Atualização de cotação

- `PUT /acoes/{id}/atualizar-cotacao` → `200`, cotação atualizada.
- Desligar a rede ou apontar `BRAPI_BASE_URL` para um host inválido e repetir → `502`, e mostrar que a cotação anterior **não foi alterada** (RN11).

## 6. Documentação e testes

- Mostrar `swagger-ui.html` com todos os endpoints documentados.
- Rodar `./mvnw test` ao vivo (ou mostrar a última execução) e destacar que **nenhuma chamada de rede real** ocorre durante a suíte (todas as integrações são substituídas por `MockWebServer`).
- Mostrar a matriz de rastreabilidade (`openspec/changes/criar-sistema-gestao-acoes/traceability.md`) ligando cada regra de negócio ao teste que a comprova.

## 7. Diferenciais — autenticação, carteira e proventos (opcional, se sobrar tempo)

Fora do escopo mínimo do enunciado, mas implementado como diferencial (seção 11: "autenticação com Spring Security").

- `POST /auth/registrar` (nome, email, senha) → `201`.
- `POST /auth/login` → `200` com um token JWT.
- Chamar `GET /carteira/lancamentos` **sem** o header `Authorization` → `401` (mostrar que corretoras/ações continuam públicas, só carteira/proventos exigem token).
- No Swagger, clicar em **Authorize** e colar `Bearer {token}`.
- `POST /carteira/lancamentos` (compra de uma ação já cadastrada, numa corretora já cadastrada) → `201`.
- `GET /carteira/posicoes` → mostra quantidade, preço médio e valor investido/atual consolidados.
- `POST /proventos` (dividendo/JCP) → `201`; `GET /proventos` → lista paginada, mais recente primeiro.
- Se o frontend Angular estiver rodando (`cd frontend && npm start` ou via Docker Compose), mostrar a mesma jornada pela interface em vez de pelo Swagger.

## 8. Rodando tudo via Docker Compose (opcional, mostra a Aula 12 aplicada)

```bash
docker compose up -d --build
docker compose ps          # postgres "healthy", aplicacao e frontend "running"
docker compose logs -f aplicacao   # ver Liquibase executando os changeSets e a app subindo
```

- Acessar `http://localhost:8080` (frontend) e `http://localhost:8080/swagger-ui.html` (API) — tudo atrás de uma única porta publicada, via proxy reverso do Nginx do frontend.
- `docker compose down` (preserva o volume) vs. `docker compose down --volumes` (apaga o banco) — explicar a diferença se perguntado.