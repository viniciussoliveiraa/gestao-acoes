# Diagramas — Sistema de Gestão de Ações

## Diagrama de entidades (simplificado)

```mermaid
erDiagram
    CORRETORA {
        bigint id PK
        varchar cnpj UK
        varchar razao_social
        varchar nome_fantasia
        varchar email
        varchar telefone
        varchar cep
        varchar logradouro
        varchar numero
        varchar complemento
        varchar bairro
        varchar cidade
        varchar uf
        varchar situacao_cadastral
        boolean validada_cvm
        timestamptz criado_em
    }

    ACAO {
        bigint id PK
        varchar ticker
        varchar nome_empresa
        varchar mercado
        varchar moeda
        numeric cotacao_atual
        timestamptz data_hora_cotacao
        varchar provedor_origem
        timestamptz criado_em
    }

    USUARIO {
        bigint id PK
        varchar nome
        varchar email UK
        varchar senha_hash
        timestamptz criado_em
    }

    LANCAMENTO {
        bigint id PK
        bigint usuario_id FK
        bigint acao_id FK
        bigint corretora_id FK
        varchar tipo
        numeric quantidade
        numeric preco_unitario
        date data_operacao
        timestamptz criado_em
    }

    PROVENTO {
        bigint id PK
        bigint usuario_id FK
        bigint acao_id FK
        varchar tipo
        numeric valor_total
        date data_pagamento
        timestamptz criado_em
    }

    USUARIO ||--o{ LANCAMENTO : registra
    ACAO ||--o{ LANCAMENTO : "é comprada em"
    CORRETORA ||--o{ LANCAMENTO : intermedia
    USUARIO ||--o{ PROVENTO : recebe
    ACAO ||--o{ PROVENTO : "paga"
```

`Corretora` e `Acao` são catálogos globais, sem dono — `Lancamento` e `Provento` são os pontos onde `Usuario` se associa a elas (ver `openspec/changes/adicionar-carteira-auth-frontend-angular/design.md`). Unicidade: `corretora.cnpj` única; `acao.ticker` única globalmente (RN07 — não por mercado); `usuario.email` única. `Posicao` (quantidade/preço médio/valor atual por ativo) não é uma tabela: é calculada em runtime agregando os `Lancamento` de cada usuário.

## Diagrama de componentes (portas e adaptadores)

```mermaid
flowchart LR
  subgraph API
    C1[CorretoraController]
    C2[AcaoController]
  end
  subgraph Service
    S1[CorretoraService]
    S2[AcaoService]
  end
  subgraph Integration
    P1[CnpjDataPort]
    P2[EnderecoPort]
    P3[InstituicaoFinanceiraPort]
    P4[CotacaoPort]
  end
  A1[BrasilApiCnpjAdapter]
  A2[ViaCepAdapter]
  A3[BrasilApiCvmAdapter]
  A4[BrapiAdapter]
  A5[TwelveDataAdapter]
  A6[AlphaVantageAdapter]
  DB[(PostgreSQL/H2)]

  C1 --> S1
  C2 --> S2
  S1 --> P1 --> A1 --> Ext1[(BrasilAPI CNPJ)]
  S1 --> P2 --> A2 --> Ext2[(ViaCEP)]
  S1 --> P3 --> A3 --> Ext3[(BrasilAPI CVM)]
  S2 --> P4
  P4 --> A4 --> Ext4[(brapi.dev)]
  P4 --> A5 --> Ext5[(Twelve Data)]
  P4 --> A6 --> Ext6[(Alpha Vantage)]
  S1 --> DB
  S2 --> DB
```

Este é o mesmo diagrama de `openspec/changes/criar-sistema-gestao-acoes/design.md`, reproduzido aqui para consulta rápida sem precisar abrir os artefatos OpenSpec. Os fluxos de sequência (cadastro de corretora, cadastro/atualização de ação) estão detalhados em `design.md`.

## Autenticação e carteira (frontend Angular)

```mermaid
flowchart LR
  UI[Angular]
  AuthC[AuthController]
  CartC[CarteiraController]
  ProvC[ProventoController]
  JwtFilter[JwtAuthenticationFilter]
  DB[(PostgreSQL/H2)]

  UI -- "POST /auth/login" --> AuthC --> DB
  AuthC -- token JWT --> UI
  UI -- "Authorization: Bearer token" --> JwtFilter
  JwtFilter -- usuarioId resolvido --> CartC
  JwtFilter -- usuarioId resolvido --> ProvC
  CartC --> DB
  ProvC --> DB
```

`/corretoras` e `/acoes` continuam públicos e não passam pelo `JwtAuthenticationFilter` como exigência — o Angular os consome normalmente, com ou sem usuário logado. Detalhes em `openspec/changes/adicionar-carteira-auth-frontend-angular/design.md`.