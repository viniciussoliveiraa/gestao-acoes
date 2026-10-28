## Purpose

Define o comportamento observável da aplicação Angular que serve como interface do sistema: autenticação de UI, navegação protegida, e as telas de Resumo, Lançamentos, Proventos, Gráficos e CRUD de Corretoras/Ações.

## ADDED Requirements

### Requirement: Tela de login e cadastro
A aplicação SHALL apresentar uma tela de login (email/senha) e uma tela de cadastro de usuário, ambas acessíveis sem autenticação prévia. Após login ou cadastro bem-sucedido, a aplicação SHALL armazenar o token recebido e redirecionar o usuário para o Resumo da carteira.

#### Scenario: Login bem-sucedido redireciona para o Resumo
- **WHEN** o usuário informa credenciais válidas na tela de login e submete o formulário
- **THEN** a aplicação armazena o token recebido e navega para a tela de Resumo

#### Scenario: Login com credenciais inválidas exibe erro
- **WHEN** o usuário submete a tela de login com credenciais inválidas
- **THEN** a aplicação exibe uma mensagem de erro compreensível, sem navegar para outra tela e sem armazenar token

### Requirement: Proteção de rotas autenticadas
A aplicação SHALL impedir o acesso às telas de Resumo, Lançamentos, Proventos e Gráficos sem um token válido armazenado, redirecionando para a tela de login nesse caso. A aplicação SHALL também redirecionar para login se uma chamada à API retornar `401` (token ausente/expirado/inválido) durante o uso.

#### Scenario: Acesso direto a rota protegida sem login
- **WHEN** um usuário não autenticado navega diretamente para a URL do Resumo da carteira
- **THEN** a aplicação redireciona para a tela de login, sem chamar a API protegida

#### Scenario: Token expira durante o uso
- **GIVEN** um usuário autenticado navegando na aplicação
- **WHEN** uma chamada à API retorna `401` por token expirado
- **THEN** a aplicação limpa o token armazenado e redireciona para a tela de login

### Requirement: Envio automático do token nas chamadas protegidas
A aplicação SHALL anexar automaticamente o header `Authorization: Bearer {token}` em toda chamada às rotas de carteira e proventos, sem exigir que cada tela implemente esse comportamento individualmente.

#### Scenario: Requisição a endpoint protegido inclui o token
- **GIVEN** um usuário autenticado com token armazenado
- **WHEN** a tela de Resumo solicita as posições da carteira
- **THEN** a requisição HTTP enviada inclui o header `Authorization` com o token armazenado

### Requirement: Tela de Resumo da carteira
A aplicação SHALL exibir, na tela de Resumo, a lista de posições consolidadas do usuário autenticado (ticker, quantidade, preço médio, valor investido, valor atual, variação), obtida da API de carteira.

#### Scenario: Resumo exibe posições existentes
- **GIVEN** um usuário autenticado com posições na carteira
- **WHEN** ele acessa a tela de Resumo
- **THEN** a aplicação exibe uma linha/card por ativo com os dados retornados pela API

#### Scenario: Resumo de carteira vazia
- **GIVEN** um usuário autenticado sem lançamentos
- **WHEN** ele acessa a tela de Resumo
- **THEN** a aplicação exibe um estado vazio explicativo, sem erro

### Requirement: Tela de Lançamentos
A aplicação SHALL prover um formulário para registrar um novo lançamento (ação, corretora, quantidade, preço, data) e uma listagem dos lançamentos já registrados pelo usuário autenticado, ambos consumindo a API de carteira.

#### Scenario: Cadastro de lançamento reflete no Resumo
- **GIVEN** um usuário autenticado na tela de Lançamentos
- **WHEN** ele submete um novo lançamento válido
- **THEN** a aplicação confirma o cadastro e a posição correspondente passa a refletir esse lançamento na próxima consulta ao Resumo

#### Scenario: Erro de validação no formulário de lançamento
- **WHEN** o usuário submete o formulário de lançamento com campos obrigatórios ausentes ou inválidos
- **THEN** a aplicação exibe a mensagem de erro retornada pela API sem navegar para outra tela

### Requirement: Tela de Proventos
A aplicação SHALL prover um formulário para registrar um provento (ação, tipo, valor, data) e uma listagem dos proventos já registrados pelo usuário autenticado, consumindo a API de proventos.

#### Scenario: Cadastro de provento
- **GIVEN** um usuário autenticado na tela de Proventos
- **WHEN** ele submete um novo provento válido
- **THEN** a aplicação confirma o cadastro e o provento passa a aparecer na listagem

### Requirement: Gráficos de alocação e evolução
A aplicação SHALL exibir, a partir dos dados de posições e lançamentos do usuário autenticado, um gráfico de alocação da carteira por ativo e um gráfico de evolução do valor investido ao longo do tempo.

#### Scenario: Gráfico de alocação reflete as posições atuais
- **GIVEN** um usuário autenticado com posições em mais de um ativo
- **WHEN** ele acessa a tela de Gráficos
- **THEN** a aplicação exibe um gráfico com a participação percentual de cada ativo no valor total investido

### Requirement: CRUD de Corretoras e Ações na interface
A aplicação SHALL prover telas para cadastrar (via CNPJ, para corretoras, e via ticker/mercado, para ações), listar e buscar corretoras e ações, consumindo os endpoints públicos existentes (`/corretoras`, `/acoes`), sem exigir autenticação para essas telas.

#### Scenario: Cadastro de corretora pela interface
- **WHEN** o usuário preenche o formulário de nova corretora com um CNPJ válido e submete
- **THEN** a aplicação exibe os dados retornados pela API (incluindo endereço preenchido via CEP) ou a mensagem de erro correspondente, sem exigir login prévio

#### Scenario: Cadastro de ação pela interface
- **WHEN** o usuário preenche o formulário de nova ação com ticker e mercado e submete
- **THEN** a aplicação exibe a ação cadastrada com a cotação obtida, ou a mensagem de erro correspondente (ex.: ticker inexistente), sem exigir login prévio

### Requirement: Exibição de erros da API
A aplicação SHALL traduzir as respostas de erro Problem Details da API (400, 404, 409, 422, 502) em mensagens compreensíveis ao usuário, sem expor o corpo técnico bruto da resposta.

#### Scenario: Erro de duplicidade exibido de forma amigável
- **WHEN** o usuário tenta cadastrar uma ação com ticker já existente e a API responde `409`
- **THEN** a aplicação exibe uma mensagem indicando que o ticker já está cadastrado, sem mostrar o JSON bruto da resposta

### Requirement: Logout
A aplicação SHALL prover uma ação de logout que remove o token armazenado e redireciona o usuário para a tela de login.

#### Scenario: Logout limpa sessão
- **GIVEN** um usuário autenticado
- **WHEN** ele aciona "Sair"
- **THEN** a aplicação remove o token armazenado e o usuário é redirecionado para a tela de login, sem mais conseguir acessar as telas protegidas até logar novamente