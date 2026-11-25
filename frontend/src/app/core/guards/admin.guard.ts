import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Só deixa entrar quem tem ROLE_ADMIN no token. É apenas conveniência de interface: quem garante
 * a regra de verdade é o backend (@PreAuthorize em UsuarioController devolve 403).
 */
export const adminGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.autenticado()) {
    router.navigate(['/login']);
    return false;
  }
  if (!authService.administrador()) {
    router.navigate(['/resumo']);
    return false;
  }
  return true;
};
