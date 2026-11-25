import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;
  let auth: { token: ReturnType<typeof vi.fn>; logout: ReturnType<typeof vi.fn> };
  let router: { navigate: ReturnType<typeof vi.fn> };

  function configurar(token: string | null): void {
    auth = { token: vi.fn().mockReturnValue(token), logout: vi.fn() };
    router = { navigate: vi.fn() };
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: auth },
        { provide: Router, useValue: router },
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
  }

  afterEach(() => backend.verify());

  it('anexa "Authorization: Bearer <token>" quando há login', () => {
    configurar('abc.def.ghi');

    http.get('/carteira/posicoes').subscribe();

    const requisicao = backend.expectOne('/carteira/posicoes');
    expect(requisicao.request.headers.get('Authorization')).toBe('Bearer abc.def.ghi');
    requisicao.flush([]);
  });

  it('sem login não envia o header', () => {
    configurar(null);

    http.get('/corretoras').subscribe();

    const requisicao = backend.expectOne('/corretoras');
    expect(requisicao.request.headers.has('Authorization')).toBe(false);
    requisicao.flush({});
  });

  it('401 desloga e manda para /login', () => {
    configurar('expirado');
    const erro = vi.fn();

    http.get('/carteira/posicoes').subscribe({ error: erro });
    backend.expectOne('/carteira/posicoes').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(auth.logout).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
    expect(erro).toHaveBeenCalled();
  });

  it('403 não desloga: só repassa o erro para a tela', () => {
    configurar('token-de-user');
    const erro = vi.fn();

    http.post('/corretoras', {}).subscribe({ error: erro });
    backend.expectOne('/corretoras').flush(null, { status: 403, statusText: 'Forbidden' });

    expect(auth.logout).not.toHaveBeenCalled();
    expect(router.navigate).not.toHaveBeenCalled();
    expect(erro).toHaveBeenCalled();
  });
});
