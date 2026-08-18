import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

/**
 * Anexa o token JWT nas chamadas para /carteira e /proventos (os únicos endpoints protegidos —
 * ver SecurityConfig no backend) e força logout em qualquer 401, independentemente da rota.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const exigeToken = req.url.includes('/carteira') || req.url.includes('/proventos');
  const token = authService.token();

  const requisicao = exigeToken && token
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