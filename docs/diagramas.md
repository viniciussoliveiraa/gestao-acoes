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
```

`Corretora` e `Acao` não têm relação direta no MVP (ver `proposal.md` — associação corretora/carteira/ação é diferencial futuro). Unicidade: `corretora.cnpj` é única; `(acao.ticker, acao.mercado)` é única em conjunto.

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