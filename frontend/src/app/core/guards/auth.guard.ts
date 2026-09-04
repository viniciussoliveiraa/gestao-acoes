import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.autenticado()) {
    return true;
  }

  // Limpa um token presente porém expirado, para não repetir o mesmo veredito a cada navegação.
  if (authService.token()) {
    authService.logout();
  }

  router.navigate(['/login']);
  return false;
};