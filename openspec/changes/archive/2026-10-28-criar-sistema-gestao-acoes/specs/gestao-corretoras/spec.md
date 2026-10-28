## Purpose

Permite cadastrar corretoras a partir do CNPJ, com dados cadastrais e endereço validados por fontes externas, e consultá-las por listagem paginada, por ID ou por CNPJ.

## ADDED Requirements

### Requirement: Cadastro de corretora por CNPJ
O sistema SHALL aceitar o cadastro de uma corretora a partir de um CNPJ, normalizando-o (somente dígitos) e validando seu formato e dígitos verificadores antes de qualquer chamada externa. O sistema SHALL rejeitar CNPJs com formato ou dígitos verificadores inválidos sem consultar provedores externos.

#### Scenario: CNPJ com dígitos verificadores inválidos
- **WHEN** o cliente envia `POST /corretoras` com um CNPJ cujos dígitos verificadores são matematicamente inválidos
- **THEN** o sistema responde `400` com Problem Details indicando o campo `cnpj` inválido, sem consultar nenhuma API externa

#### Scenario: CNPJ com máscara é normalizado antes da validação
- **WHEN** o cliente envia `POST /corretoras` com `cnpj` formatado como `12.345.678/0001-95`
- **THEN** o sistema normaliza para 14 dígitos antes de validar e de consultar os provedores externos

### Requirement: Enriquecimento cadastral obrigatório via provedor externo
O sistema SHALL obter `razaoSocial`, `nomeFantasia` e `situacaoCadastral` da corretora a partir da integração de dados empresariais (`integracao-dados-empresariais`) e NÃO SHALL aceitar o preenchimento manual desses campos como substituto da consulta externa.

#### Scenario: CNPJ válido mas inexistente no provedor
- **WHEN** o cliente cadastra um CNPJ com formato válido que o provedor de dados empresariais não encontra
- **THEN** o sistema responde `404` ou `422` (conforme definido em `design.md`) informando que o CNPJ não foi localizado, e nenhuma corretora é persistida

### Requirement: Corretora deve ser validada como instituição financeira
O sistema SHALL delegar à capacidade `validacao-instituicao-financeira` a verificação de que o CNPJ corresponde a uma instituição compatível com atuação no mercado financeiro antes de persistir a corretora. Quando a instituição não for reconhecida, o sistema SHALL rejeitar o cadastro com `422`, conforme a política RN03.

#### Scenario: Empresa existe mas não é instituição financeira reconhecida
- **WHEN** o CNPJ é válido e encontrado na BrasilAPI, mas não consta como corretora ativa na fonte da CVM
- **THEN** o sistema responde `422` com Problem Details explicando que a instituição não foi validada como participante do mercado financeiro, e nenhuma corretora é persistida

### Requirement: Endereço obtido e validado por CEP
O sistema SHALL exigir um CEP no cadastro, validá-lo por meio da capacidade `integracao-enderecos` e preencher `logradouro`, `bairro`, `cidade` e `uf` a partir da resposta externa antes de salvar a corretora.

#### Scenario: CEP com formato inválido
- **WHEN** o cliente envia um CEP que não possui 8 dígitos numéricos
- **THEN** o sistema responde `400` com Problem Details indicando o campo `cep` inválido

#### Scenario: CEP válido mas inexistente
- **WHEN** o cliente envia um CEP com 8 dígitos que a ViaCEP retorna com `erro: true`
- **THEN** o sistema responde `422` informando que o CEP não foi encontrado, e nenhuma corretora é persistida

### Requirement: Unicidade de CNPJ
O sistema SHALL impedir o cadastro de duas corretoras com o mesmo CNPJ normalizado, mesmo quando os CNPJs forem enviados com máscaras diferentes. Essa restrição SHALL ser garantida tanto na camada de aplicação quanto por constraint única no banco de dados.

#### Scenario: Cadastro duplicado com máscara diferente
- **GIVEN** uma corretora já cadastrada com CNPJ `12345678000195`
- **WHEN** o cliente envia `POST /corretoras` com `cnpj` `12.345.678/0001-95`
- **THEN** o sistema responde `409` com Problem Details indicando duplicidade de CNPJ, e nenhuma nova corretora é criada

#### Scenario: Cadastros concorrentes do mesmo CNPJ
- **WHEN** duas requisições `POST /corretoras` com o mesmo CNPJ chegam simultaneamente
- **THEN** apenas uma corretora é persistida e a segunda requisição recebe `409`, com a violação de constraint única do banco traduzida para a resposta padronizada

### Requirement: Persistência atômica do cadastro
O sistema NÃO SHALL persistir uma corretora parcialmente cadastrada. Se qualquer etapa de validação externa (dados empresariais, validação de instituição financeira ou CEP) falhar, nenhuma linha SHALL ser gravada.

#### Scenario: Falha do provedor de CEP após validação bem-sucedida do CNPJ
- **WHEN** o CNPJ é validado com sucesso, mas a consulta ao CEP falha por indisponibilidade do provedor
- **THEN** o sistema responde `502` ou `503` e nenhuma corretora é persistida, mesmo que os dados do CNPJ já tenham sido obtidos

### Requirement: Consulta de corretoras
O sistema SHALL permitir listar corretoras com paginação, buscar uma corretora por ID e buscar uma corretora por CNPJ (aceitando o CNPJ com ou sem máscara, normalizando antes da busca).

#### Scenario: Listagem paginada
- **WHEN** o cliente chama `GET /corretoras?page=0&size=20`
- **THEN** o sistema responde `200` com uma página de corretoras e metadados de paginação (total de elementos, total de páginas, página atual)

#### Scenario: Busca por ID inexistente
- **WHEN** o cliente chama `GET /corretoras/{id}` com um ID que não existe
- **THEN** o sistema responde `404` com Problem Details

#### Scenario: Busca por CNPJ com máscara
- **WHEN** o cliente chama `GET /corretoras/cnpj/12.345.678/0001-95`
- **THEN** o sistema normaliza o CNPJ recebido e retorna `200` com a corretora correspondente, se existir, ou `404` caso contrário