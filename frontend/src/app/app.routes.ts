import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { Login } from './features/auth/login/login';
import { Registro } from './features/auth/registro/registro';
import { Shell } from './features/shell/shell';

export const routes: Routes = [
  { path: 'login', component: Login },
  { path: 'registro', component: Registro },
  {
    path: '',
    component: Shell,
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'resumo' },
      {
        path: 'resumo',
        canActivate: [authGuard],
        data: { titulo: 'Resumo', icone: 'dashboard' },
        loadComponent: () => import('./features/resumo/resumo').then((m) => m.Resumo),
      },
      {
        path: 'lancamentos',
        canActivate: [authGuard],
        data: { titulo: 'Lançamentos', icone: 'receipt_long' },
        loadComponent: () => import('./features/lancamentos/lancamentos').then((m) => m.Lancamentos),
      },
      {
        path: 'proventos',
        canActivate: [authGuard],
        data: { titulo: 'Proventos', icone: 'payments' },
        loadComponent: () => import('./features/proventos/proventos').then((m) => m.Proventos),
      },
      {
        path: 'graficos',
        canActivate: [authGuard],
        data: { titulo: 'Gráficos', icone: 'pie_chart' },
        loadComponent: () => import('./features/graficos/graficos').then((m) => m.Graficos),
      },
      {
        path: 'rebalanceamento',
        canActivate: [authGuard],
        data: { titulo: 'Rebalanceamento', icone: 'balance' },
        loadComponent: () => import('./features/rebalanceamento/rebalanceamento').then((m) => m.Rebalanceamento),
      },
      {
        path: 'corretoras',
        data: { titulo: 'Corretoras', icone: 'account_balance' },
        loadComponent: () => import('./features/corretoras/corretoras').then((m) => m.Corretoras),
      },
      {
        path: 'acoes',
        data: { titulo: 'Ações', icone: 'trending_up' },
        loadComponent: () => import('./features/acoes/acoes').then((m) => m.Acoes),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];