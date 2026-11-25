# Frontend — Sistema de Gestão de Ações

Interface Angular (standalone components, Angular 22) para o [backend Spring Boot](../README.md): login/cadastro, dashboard de carteira (Resumo, Lançamentos, Proventos, Gráficos, Rebalanceamento) cadastro/listagem de Corretoras/Ações e, para administradores, gerenciamento de usuários (login, senha, papel e situação). Gerado com Angular CLI e detalhado em `openspec/changes/adicionar-carteira-auth-frontend-angular/`.

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
3. **Corretoras** e **Ações** podem ser consultadas sem login; cadastrá-las exige login como administrador (Aula 14 — RBAC; só cadastro + listagem, sem edição/exclusão) — cadastre ao menos uma de cada para poder registrar lançamentos.
4. **Lançamentos**: registre uma compra ou venda (tipo + ação + corretora + quantidade + preço + data) — ao escolher a ação, o preço unitário já vem preenchido com a cotação atual dela (pode ser ajustado). Venda é validada contra o saldo disponível do ativo. Lançamentos já registrados podem ser excluídos (recalcula a posição do ativo); ainda não há edição.
5. **Resumo**: mostra a posição consolidada calculada a partir dos lançamentos.
6. **Proventos**: registre dividendos/JCP recebidos.
7. **Gráficos**: alocação por ativo e evolução do valor investido.
8. **Rebalanceamento**: defina metas de alocação (%) por ativo e veja a sugestão de aporte para se aproximar da meta; as metas ficam salvas só no navegador (`localStorage`), não no backend.
9. **Modo noturno**: o botão de lua/sol na barra do topo alterna entre modo claro e escuro; a escolha fica salva no navegador (sem escolha, segue o modo do sistema operacional).
10. **Usuários** (só aparece para administradores, em "Administração"): lista os usuários; em **Editar** é possível alterar o login (email), nome, papel e situação (ativo/inativo) e, em outro formulário, definir uma nova senha (8 a 72 caracteres, com confirmação).

## Estrutura

```
src/app/
  core/
    guards/        authGuard (protege Resumo/Lançamentos/Proventos/Gráficos/Rebalanceamento), adminGuard (Usuários)
    i18n/          textos do paginator do Angular Material em pt-BR
    interceptors/  authInterceptor (anexa Bearer token, trata 401 global)
    models/        interfaces TypeScript espelhando os DTOs do backend
    services/      um serviço HTTP por recurso (Auth, Carteira, Provento, Corretora, Acao, Usuario) + TemaService (modo claro/escuro)
    testing/       apoio só para os testes (dados de exemplo, JWT falso) — fora do build
  features/
    auth/          login, registro
    shell/         layout com sidebar/toolbar (envolve as telas autenticadas e públicas)
    resumo/        dashboard de posições
    lancamentos/   formulário + histórico de compras e vendas
    proventos/     formulário + histórico de proventos
    graficos/      alocação (donut) e evolução (linha), via ng2-charts/Chart.js
    rebalanceamento/ metas de alocação por ativo (localStorage) e sugestão de aporte
    corretoras/    cadastro e listagem de corretoras (público, sem edição/exclusão)
    acoes/         cadastro e listagem de ações (público, sem edição/exclusão)
    usuarios/      administração de usuários: alterar login, senha, papel e situação (só ADMIN)
```

## Build de produção

```bash
ng build
```

Gera os artefatos em `dist/frontend/`. Não há integração automática com o build Maven do backend neste MVP — os dois projetos são deployados/rodados separadamente (ver decisão em `openspec/changes/adicionar-carteira-auth-frontend-angular/design.md`).

## Testes

```bash
npm run test:coverage   # testes + relatório + portão de cobertura (85%)
npx ng test             # modo interativo: roda de novo a cada arquivo salvo
```

Suíte do Angular CLI com Vitest (ambiente jsdom) e cobertura via `@vitest/coverage-v8`: **165 testes**, cobertura de **99,81% statements, 97,00% ramos, 99,71% funções e 100% linhas**. O relatório HTML fica em `coverage/frontend/index.html`.

- **Portão**: `coverageThresholds` no `angular.json` exige 85% nas quatro métricas; abaixo disso o comando termina com erro (o equivalente ao `check` do JaCoCo no backend).
- **Conta honesta**: `coverageInclude` inclui todos os arquivos de `src/app`, não só os que algum teste importou.
- **Onde estão**: um `.spec.ts` ao lado de cada parte testada; telas agrupadas em `features/cadastros.spec.ts` (Corretoras, Ações, Lançamentos, Proventos) e `features/carteira.spec.ts` (Resumo, Reequilíbrio).
- **Apoio**: `src/test-setup.ts` simula `window.matchMedia` (o jsdom não tem); `core/testing/dados.ts` tem dados de exemplo e o erro HTTP simulado; `core/testing/jwt-falso.ts` monta tokens para testar login/papel.
- Nenhum teste chama a API de verdade (serviços simulados e `HttpTestingController`), e nada disso entra no build de produção.
- **CI**: o job "frontend (Vitest)" de `.github/workflows/testes.yml` roda `npm ci` + `npm run test:coverage` a cada push e publica o relatório como artefato `relatorio-cobertura-frontend`.

## Limitações conhecidas

- Sem refresh token — o JWT expira (`APP_JWT_EXPIRATION_MINUTES` no backend) e força novo login.
- Cada usuário (inclusive o administrador) só enxerga os próprios lançamentos/proventos; o papel `ADMIN` libera o cadastro de corretoras/ações e a tela de Usuários.
- Ao trocar o próprio email, o nome exibido no topo só muda no próximo login (o token atual continua válido até expirar).