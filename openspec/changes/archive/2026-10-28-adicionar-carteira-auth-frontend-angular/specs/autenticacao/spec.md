## Purpose

Permite que um usuário se cadastre e faça login para obter um token de acesso, e garante que os endpoints de carteira e proventos só sejam acessíveis a usuários autenticados, sem alterar o acesso público existente a corretoras e ações.

## ADDED Requirements

### Requirement: Cadastro de usuário
O sistema SHALL aceitar `POST /auth/registrar` recebendo `nome`, `email` e `senha`, armazenando a senha apenas na forma de hash (nunca em texto puro) e impedindo o cadastro de dois usuários com o mesmo `email` normalizado (minúsculas, sem espaços nas extremidades).

#### Scenario: Cadastro bem-sucedido
- **WHEN** o cliente envia `POST /auth/registrar` com `email` inédito e `senha` válida
- **THEN** o sistema responde `201`, cria o usuário com a senha em hash, e não retorna a senha (nem seu hash) no corpo da resposta

#### Scenario: Email já cadastrado
- **GIVEN** um usuário já cadastrado com `email: "ana@exemplo.com"`
- **WHEN** o cliente envia `POST /auth/registrar` com o mesmo `email` (mesmo com variação de maiúsculas/minúsculas)
- **THEN** o sistema responde `409` com Problem Details, e nenhum novo usuário é criado

#### Scenario: Senha muito curta
- **WHEN** o cliente envia `POST /auth/registrar` com uma senha abaixo do tamanho mínimo definido
- **THEN** o sistema responde `400` com Problem Details detalhando a violação, e nenhum usuário é criado

### Requirement: Login e emissão de token
O sistema SHALL aceitar `POST /auth/login` recebendo `email` e `senha`; se as credenciais forem válidas, SHALL responder `200` com um token de acesso (JWT) com prazo de expiração definido em `design.md`. Se as credenciais forem inválidas, o sistema NÃO SHALL indicar se o problema foi o email ou a senha, respondendo de forma genérica.

#### Scenario: Login com credenciais válidas
- **GIVEN** um usuário cadastrado com `email: "ana@exemplo.com"` e senha `"Senha123!"`
- **WHEN** o cliente envia `POST /auth/login` com essas credenciais
- **THEN** o sistema responde `200` com um token válido

#### Scenario: Login com senha incorreta
- **GIVEN** um usuário cadastrado com `email: "ana@exemplo.com"`
- **WHEN** o cliente envia `POST /auth/login` com a senha errada
- **THEN** o sistema responde `401` com Problem Details genérico, sem revelar se o email existe

#### Scenario: Login com email não cadastrado
- **WHEN** o cliente envia `POST /auth/login` com um `email` que não existe no sistema
- **THEN** o sistema responde `401` com o mesmo formato de erro genérico usado para senha incorreta

### Requirement: Proteção dos endpoints de carteira e proventos
O sistema SHALL exigir um token válido (via header `Authorization: Bearer {token}`) para qualquer requisição a `/carteira/**` e `/proventos/**`. Requisições sem token, com token malformado, inválido ou expirado SHALL ser rejeitadas com `401`, no mesmo formato Problem Details usado no restante da API.

#### Scenario: Requisição sem token
- **WHEN** o cliente chama `GET /carteira/posicoes` sem header `Authorization`
- **THEN** o sistema responde `401` com Problem Details, sem executar a consulta

#### Scenario: Token expirado
- **WHEN** o cliente chama um endpoint protegido com um token cujo prazo de expiração já passou
- **THEN** o sistema responde `401`, indicando que o token não é mais válido

### Requirement: Resolução do usuário autenticado
O sistema SHALL identificar o usuário autenticado a partir das claims do token válido, e SHALL usar esse identificador para escopar todas as operações de leitura e escrita em `/carteira/**` e `/proventos/**` — nunca aceitando um identificador de usuário vindo do corpo da requisição ou de parâmetro de URL para esse fim.

#### Scenario: Usuário autenticado só acessa os próprios dados
- **GIVEN** dois usuários cadastrados, cada um com lançamentos próprios na carteira
- **WHEN** o usuário A autenticado chama `GET /carteira/posicoes`
- **THEN** o sistema responde apenas com as posições calculadas a partir dos lançamentos do usuário A, nunca do usuário B

### Requirement: Endpoints existentes permanecem públicos
O sistema NÃO SHALL exigir autenticação para os endpoints já existentes de `/corretoras/**` e `/acoes/**` (RF01–RF12 do change `criar-sistema-gestao-acoes`), preservando o comportamento e os contratos já implementados e testados.

#### Scenario: Consulta de ações sem autenticação continua funcionando
- **WHEN** um cliente chama `GET /acoes` sem header `Authorization`
- **THEN** o sistema responde `200` normalmente, sem exigir token