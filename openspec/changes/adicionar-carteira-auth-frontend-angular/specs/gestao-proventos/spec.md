## Purpose

Permite que um usuário autenticado registre e consulte proventos (dividendos/JCP) recebidos por ativo da sua carteira.

## ADDED Requirements

### Requirement: Registro de provento
O sistema SHALL aceitar `POST /proventos`, autenticado, recebendo `acaoId` (ou `ticker`), `tipo` (`DIVIDENDO` ou `JCP`), `valorTotal` e `dataPagamento`, associando o provento ao usuário autenticado. O sistema SHALL validar que a ação informada existe antes de persistir o provento.

#### Scenario: Registro bem-sucedido
- **GIVEN** uma ação `PETR4` já cadastrada no sistema
- **WHEN** o usuário autenticado envia `POST /proventos` com `acaoId`, `tipo: "DIVIDENDO"`, `valorTotal: 45.90`, `dataPagamento: "2026-07-15"`
- **THEN** o sistema responde `201` com o provento criado, associado ao usuário autenticado

#### Scenario: Ação inexistente
- **WHEN** o usuário autenticado envia `POST /proventos` referenciando um `acaoId` que não existe
- **THEN** o sistema responde `404` com Problem Details, e nenhum provento é persistido

#### Scenario: Valor inválido
- **WHEN** o usuário autenticado envia `POST /proventos` com `valorTotal` menor ou igual a zero
- **THEN** o sistema responde `400` com Problem Details, e nenhum provento é persistido

### Requirement: Listagem de proventos do usuário
O sistema SHALL aceitar `GET /proventos`, autenticado, retornando os proventos do usuário autenticado, paginados e ordenados por `dataPagamento` decrescente.

#### Scenario: Listagem paginada
- **WHEN** o usuário autenticado chama `GET /proventos?page=0&size=20`
- **THEN** o sistema responde `200` com uma página contendo apenas proventos daquele usuário, do mais recente para o mais antigo

#### Scenario: Nenhum provento registrado
- **GIVEN** um usuário autenticado sem proventos registrados
- **WHEN** ele chama `GET /proventos`
- **THEN** o sistema responde `200` com uma lista vazia

### Requirement: Isolamento de proventos entre usuários
O sistema NÃO SHALL expor proventos de um usuário a outro usuário autenticado.

#### Scenario: Usuário não vê proventos de outro usuário
- **GIVEN** os usuários A e B, cada um com proventos próprios registrados
- **WHEN** o usuário B chama `GET /proventos`
- **THEN** o sistema retorna apenas os proventos pertencentes ao usuário B