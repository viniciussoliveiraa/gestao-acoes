## Purpose

Define o formato centralizado de erro da API e as regras de resiliência (timeouts, retry, rate limit, atomicidade de persistência) aplicáveis a todas as integrações externas e operações de escrita.

## ADDED Requirements

### Requirement: Formato centralizado de erro
Toda resposta de erro da API SHALL seguir um formato consistente, preferencialmente Problem Details (RFC 9457), contendo status, tipo/código, título, detalhe, caminho da requisição, timestamp e, quando aplicável, a lista de erros de campo. O sistema NÃO SHALL expor stack trace, segredo, URL assinada ou corpo bruto de resposta de fornecedor externo nessas respostas.

#### Scenario: Erro de validação de campo
- **WHEN** o cliente envia um payload que falha em uma validação de Bean Validation
- **THEN** o sistema responde `400` no formato Problem Details, com a lista de campos inválidos e suas mensagens, sem stack trace

#### Scenario: Falha de provedor externo não vaza detalhes internos
- **WHEN** uma integração externa falha e o sistema traduz isso em `502` ou `503`
- **THEN** a resposta ao cliente não contém o corpo bruto, cabeçalhos de autenticação ou URL completa (com querystring sensível) da chamada ao provedor

### Requirement: Mapeamento de códigos HTTP por tipo de falha
O sistema SHALL usar `201` para criação bem-sucedida, `200` para consulta/atualização bem-sucedida, `400` para entrada inválida, `404` para recurso ou ticker inexistente, `409` para duplicidade e `502` ou `503` para dependência externa indisponível, conforme a decisão registrada em `design.md`.

#### Scenario: Recurso inexistente
- **WHEN** o cliente consulta um recurso por ID que não existe
- **THEN** o sistema responde `404`

#### Scenario: Duplicidade detectada pelo banco
- **WHEN** uma violação de constraint única ocorre durante a persistência (corretora ou ação duplicada)
- **THEN** o sistema traduz a exceção de persistência em uma resposta `409` no formato padronizado, sem propagar a exceção nativa do driver/ORM

### Requirement: Timeouts em toda chamada externa
Toda chamada a um provedor externo (BrasilAPI, ViaCEP, brapi.dev, Twelve Data, Alpha Vantage) SHALL ter timeout de conexão e de leitura configuráveis. O sistema NÃO SHALL permitir que uma chamada externa bloqueie uma requisição indefinidamente.

#### Scenario: Provedor não responde
- **WHEN** um provedor externo não responde dentro do timeout configurado
- **THEN** a requisição falha de forma controlada e o cliente recebe `502` ou `503` dentro de um tempo limitado

### Requirement: Retry limitado a falhas transitórias e operações idempotentes
Quando implementado, o retry SHALL se aplicar apenas a falhas transitórias (timeout, erro de conexão, `5xx` esporádico) em operações idempotentes (consultas), com backoff e um limite baixo de tentativas. O sistema NÃO SHALL repetir automaticamente operações de escrita não idempotentes nem respostas `429`.

#### Scenario: Retry em consulta com falha transitória
- **WHEN** uma consulta de cotação falha uma vez por erro de conexão e o provedor responde com sucesso na tentativa seguinte, dentro do limite configurado
- **THEN** o sistema completa a operação com sucesso sem expor a falha intermediária ao cliente

#### Scenario: Rate limit não é tratado como falha transitória retentável em loop
- **WHEN** um provedor responde `429`
- **THEN** o sistema não entra em um loop de novas tentativas imediatas e retorna um erro explícito ao cliente dentro de um tempo limitado

### Requirement: Nenhuma persistência parcial
Nenhuma falha de provedor externo SHALL resultar em persistência parcial de uma corretora ou de uma ação. Todas as operações de escrita que dependem de validação externa SHALL ocorrer dentro de uma transação que só é confirmada após todas as validações externas obrigatórias serem concluídas com sucesso.

#### Scenario: Falha após obtenção parcial de dados
- **WHEN** os dados do CNPJ foram obtidos com sucesso, mas a validação de instituição financeira falha em seguida
- **THEN** nenhuma linha de corretora é gravada no banco

### Requirement: Segredos nunca expostos em log
Logs estruturados SHALL incluir um correlation ID por requisição e NÃO SHALL registrar CNPJ completo, tokens, chaves de API ou outros dados sensíveis em texto claro.

#### Scenario: Log de uma chamada externa com falha
- **WHEN** uma chamada a um provedor externo falha e o sistema registra um log de erro
- **THEN** o log contém o correlation ID e a natureza do erro, mas não contém a chave de API usada nem o CNPJ completo em texto claro