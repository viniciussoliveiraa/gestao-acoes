## Purpose

Garante que a API seja documentada, reproduzível e observável o suficiente para avaliação acadêmica: OpenAPI acessível, README com instruções completas, e limitações de cada integração externa registradas.

## ADDED Requirements

### Requirement: Documentação OpenAPI acessível
O sistema SHALL expor documentação OpenAPI/Swagger UI descrevendo todos os endpoints, payloads, códigos de resposta e exemplos de sucesso e erro para `/corretoras` e `/acoes`.

#### Scenario: Acesso à documentação
- **WHEN** a aplicação está em execução e o cliente acessa o endpoint do Swagger UI
- **THEN** o sistema responde `200` com a documentação interativa de todos os endpoints especificados em `gestao-corretoras` e `gestao-acoes`

### Requirement: README reproduzível
O `README.md` SHALL conter pré-requisitos, configuração no IntelliJ, variáveis de ambiente, perfis disponíveis, instruções de execução e de testes, e limitações conhecidas de cada API externa (data da verificação, plano utilizado, autenticação, limites conhecidos, freshness da cotação e limitações de licença/uso).

#### Scenario: Seguir o README do zero
- **WHEN** um novo desenvolvedor segue as instruções do `README.md` em um ambiente limpo com as variáveis de ambiente documentadas
- **THEN** a aplicação sobe com sucesso e os endpoints documentados respondem conforme especificado

### Requirement: Exemplo de configuração sem segredos
O repositório SHALL conter um arquivo `.env.example` listando todas as variáveis de ambiente necessárias, sem valores secretos reais. Nenhum segredo real SHALL ser versionado no repositório.

#### Scenario: Verificação de segredos no repositório
- **WHEN** o repositório é inspecionado em busca de chaves de API ou credenciais versionadas
- **THEN** nenhuma chave real é encontrada, apenas placeholders em `.env.example`

### Requirement: Artefatos de apresentação
O projeto SHALL incluir uma coleção Postman ou Insomnia com variáveis de ambiente e exemplos de todos os endpoints, um diagrama simplificado de entidades e um diagrama de componentes destacando portas e adaptadores.

#### Scenario: Importação da coleção de requisições
- **WHEN** a coleção Postman/Insomnia é importada com as variáveis de ambiente configuradas
- **THEN** todas as requisições de exemplo (corretoras e ações) podem ser executadas contra uma instância local da aplicação