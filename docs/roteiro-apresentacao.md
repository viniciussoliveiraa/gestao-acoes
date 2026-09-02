# Roteiro de apresentação prática

Pré-requisito: aplicação rodando (`./mvnw spring-boot:run`, perfil `dev` com PostgreSQL ou perfil `test` com H2 para uma demonstração rápida sem infraestrutura). Swagger UI em `http://localhost:8080/swagger-ui.html`.

## 1. Cadastro de corretora — caminho feliz

`POST /corretoras` com um CNPJ real de corretora ativa (ex.: `44527444000155`, ABN AMRO Clearing) e um CEP válido (ex.: `01310100`).

- Mostrar que a resposta (`201`) traz `razaoSocial`, `situacaoCadastral` e endereço **vindos das APIs externas**, não do payload enviado.
- Repetir a mesma chamada → `409` (CNPJ duplicado).

## 2. Cadastro de corretora — cenários de erro

- CNPJ com dígito verificador errado → `400`, sem nenhuma chamada externa (mostrar nos logs/Swagger que a resposta é imediata).
- CNPJ de uma empresa que existe mas não é corretora (qualquer CNPJ comum) → `422` (RN03: instituição não validada na CVM).
- CEP inexistente com formato válido (ex.: `99999999`) → `422`.

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