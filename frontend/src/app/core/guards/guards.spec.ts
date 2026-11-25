import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { adminGuard } from './admin.guard';
import { authGuard } from './auth.guard';

/** Dublê do AuthService: os guards só leem estes quatro membros. */
function authFalso(opcoes: { autenticado: boolean; administrador?: boolean; token?: string | null }) {
  return {
    autenticado: () => opcoes.autenticado,
    administrador: () => opcoes.administrador ?? false,
    token: () => opcoes.token ?? null,
    logout: vi.fn(),
  };
}

describe('Guards de rota', () => {
  let router: { navigate: ReturnType<typeof vi.fn> };

  function executar(guard: typeof authGuard, auth: ReturnType<typeof authFalso>): unknown {
    router = { navigate: vi.fn() };
    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: auth },
        { provide: Router, useValue: router },
      ],
    });
    return TestBed.runInInjectionContext(() =>
      guard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot)
    );
  }

  describe('authGuard', () => {
    it('libera quem está autenticado', () => {
      expect(executar(authGuard, authFalso({ autenticado: true }))).toBe(true);
      expect(router.navigate).not.toHaveBeenCalled();
    });

    it('sem login manda para /login', () => {
      expect(executar(authGuard, authFalso({ autenticado: false }))).toBe(false);
      expect(router.navigate).toHaveBeenCalledWith(['/login']);
    });

    it('token expirado é limpo (logout) antes de mandar para /login', () => {
      const auth = authFalso({ autenticado: false, token: 'expirado' });

      expect(executar(authGuard, auth)).toBe(false);
      expect(auth.logout).toHaveBeenCalled();
      expect(router.navigate).toHaveBeenCalledWith(['/login']);
    });
  });

  describe('adminGuard', () => {
    it('libera ADMIN autenticado', () => {
      expect(executar(adminGuard, authFalso({ autenticado: true, administrador: true }))).toBe(true);
    });

    it('USER autenticado volta para /resumo', () => {
      expect(executar(adminGuard, authFalso({ autenticado: true, administrador: false }))).toBe(false);
      expect(router.navigate).toHaveBeenCalledWith(['/resumo']);
    });

    it('sem login manda para /login', () => {
      expect(executar(adminGuard, authFalso({ autenticado: false }))).toBe(false);
      expect(router.navigate).toHaveBeenCalledWith(['/login']);
    });
  });
});
