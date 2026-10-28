import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

/**
 * Anexa o token JWT (quando existir) em toda chamada à API. Antes da Aula 14 (RBAC), só
 * /carteira e /proventos exigiam autenticação, então o token só era anexado nessas rotas; agora
 * criar/atualizar corretoras e ações também exige um usuário ADMIN autenticado (GET continua
 * público), então restringir por URL deixaria de funcionar silenciosamente nesses dois
 * formulários. Anexar sempre que houver token é seguro: endpoints públicos simplesmente ignoram
 * o header (ver SecurityConfig no backend), e o próprio backend decide o que exige o quê.
 * Força logout em qualquer 401 (sessão ausente/expirada/inválida) — 403 (autenticado mas sem
 * permissão, ex.: USER tentando cadastrar corretora) não desloga, só propaga o erro para a tela
 * tratar.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const token = authService.token();

  const requisicao = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(requisicao).pipe(
    catchError((erro: HttpErrorResponse) => {
      if (erro.status === 401) {
        authService.logout();
        router.navigate(['/login']);
      }
      return throwError(() => erro);
    })
  );
};