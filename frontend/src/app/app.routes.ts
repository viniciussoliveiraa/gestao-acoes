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
        loadComponent: () => import('./features/resumo/resumo').then((m) => m.Resumo),
      },
      {
        path: 'lancamentos',
        canActivate: [authGuard],
        loadComponent: () => import('./features/lancamentos/lancamentos').then((m) => m.Lancamentos),
      },
      {
        path: 'proventos',
        canActivate: [authGuard],
        loadComponent: () => import('./features/proventos/proventos').then((m) => m.Proventos),
      },
      {
        path: 'graficos',
        canActivate: [authGuard],
        loadComponent: () => import('./features/graficos/graficos').then((m) => m.Graficos),
      },
      {
        path: 'corretoras',
        loadComponent: () => import('./features/corretoras/corretoras').then((m) => m.Corretoras),
      },
      {
        path: 'acoes',
        loadComponent: () => import('./features/acoes/acoes').then((m) => m.Acoes),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];