# Frontend — Sistema de Gestão de Ações

Interface Angular (standalone components, Angular 22) para o [backend Spring Boot](../README.md): login/cadastro, dashboard de carteira (Resumo, Lançamentos, Proventos, Gráficos, Rebalanceamento) e cadastro/listagem de Corretoras/Ações. Gerado com Angular CLI e detalhado em `openspec/changes/adicionar-carteira-auth-frontend-angular/`.

## Pré-requisitos

- Node.js 20+ e npm
- Backend rodando em `http://localhost:8080` (ver `../README.md`) — sem ele, nenhuma tela funciona, já que o frontend não tem lógica própria além de consumir a API

## Executando em desenvolvimento

```bash
npm install
npm start   # equivalente a `ng serve`, sobe em http://localhost:4200
```

`src/environments/environment.development.ts` aponta `apiUrl` para `http://localhost:8080`. O backend precisa liberar essa origem via CORS (`APP_CORS_ALLOWED_ORIGINS`, já configurado com esse padrão — ver `.env.example` do backend).

## Fluxo de uso

1. Abra `http://localhost:4200` → redireciona para `/login`.
2. Cadastre-se em "Cadastre-se" ou faça login (token JWT armazenado no navegador).
3. **Corretoras** e **Ações** podem ser cadastradas sem login (endpoints públicos no backend, só cadastro + listagem — sem edição/exclusão) — cadastre ao menos uma de cada para poder registrar lançamentos.
4. **Lançamentos**: registre uma compra (ação + corretora + quantidade + preço + data).
5. **Resumo**: mostra a posição consolidada calculada a partir dos lançamentos.
6. **Proventos**: registre dividendos/JCP recebidos.
7. **Gráficos**: alocação por ativo e evolução do valor investido.
8. **Rebalanceamento**: defina metas de alocação (%) por ativo e veja a sugestão de aporte para se aproximar da meta; as metas ficam salvas só no navegador (`localStorage`), não no backend.

## Estrutura

```
src/app/
  core/
    guards/        authGuard (protege Resumo/Lançamentos/Proventos/Gráficos/Rebalanceamento)
    i18n/          textos do paginator do Angular Material em pt-BR
    interceptors/  authInterceptor (anexa Bearer token, trata 401 global)
    models/        interfaces TypeScript espelhando os DTOs do backend
    services/      um serviço HTTP por recurso (Auth, Carteira, Provento, Corretora, Acao)
  features/
    auth/          login, registro
    shell/         layout com sidebar/toolbar (envolve as telas autenticadas e públicas)
    resumo/        dashboard de posições
    lancamentos/   formulário + histórico de compras
    proventos/     formulário + histórico de proventos
    graficos/      alocação (donut) e evolução (linha), via ng2-charts/Chart.js
    rebalanceamento/ metas de alocação por ativo (localStorage) e sugestão de aporte
    corretoras/    cadastro e listagem de corretoras (público, sem edição/exclusão)
    acoes/         cadastro e listagem de ações (público, sem edição/exclusão)
```

## Build de produção

```bash
ng build
```

Gera os artefatos em `dist/frontend/`. Não há integração automática com o build Maven do backend neste MVP — os dois projetos são deployados/rodados separadamente (ver decisão em `openspec/changes/adicionar-carteira-auth-frontend-angular/design.md`).

## Testes

```bash
ng test
```

Roda a suíte padrão do Angular CLI (Vitest). Cobertura mínima — o esforço de testes automatizados deste projeto está concentrado no backend (121 testes).

## Limitações conhecidas

- Sem venda/baixa de posição (carteira só registra compras/aportes).
- Sem refresh token — o JWT expira (`APP_JWT_EXPIRATION_MINUTES` no backend) e força novo login.
- Sem autorização por papéis — todo usuário autenticado só enxerga os próprios lançamentos/proventos.