import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { expiraEmSegundos, jwtFalso } from '../testing/jwt-falso';
import { AuthService } from './auth.service';

const CHAVE_TOKEN = 'gestao-acoes.token';

describe('AuthService', () => {
  let http: HttpTestingController;

  /** O serviço lê o token salvo no construtor, então o storage é preparado antes de criá-lo. */
  function criarServico(tokenSalvo?: string): AuthService {
    if (tokenSalvo) {
      localStorage.setItem(CHAVE_TOKEN, tokenSalvo);
    }
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    return TestBed.inject(AuthService);
  }

  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
  });

  afterEach(() => http?.verify());

  it('sem token salvo não está autenticado e não tem email, id nem papel de admin', () => {
    const servico = criarServico();

    expect(servico.autenticado()).toBe(false);
    expect(servico.emailUsuario()).toBeNull();
    expect(servico.idUsuario()).toBeNull();
    expect(servico.administrador()).toBe(false);
  });

  it('com token válido de ADMIN lê email, id e papel do payload', () => {
    const token = jwtFalso({
      sub: '10',
      email: 'admin@gestao-acoes.local',
      authorities: ['ROLE_ADMIN'],
      exp: expiraEmSegundos(3600),
    });

    const servico = criarServico(token);

    expect(servico.autenticado()).toBe(true);
    expect(servico.emailUsuario()).toBe('admin@gestao-acoes.local');
    expect(servico.idUsuario()).toBe(10);
    expect(servico.administrador()).toBe(true);
  });

  it('usuário comum (ROLE_USER) não é administrador', () => {
    const token = jwtFalso({ sub: '2', authorities: ['ROLE_USER'], exp: expiraEmSegundos(3600) });

    expect(criarServico(token).administrador()).toBe(false);
  });

  it('token expirado não conta como autenticado nem como administrador', () => {
    const token = jwtFalso({ sub: '10', authorities: ['ROLE_ADMIN'], exp: expiraEmSegundos(-60) });

    const servico = criarServico(token);

    expect(servico.autenticado()).toBe(false);
    expect(servico.administrador()).toBe(false);
  });

  it('token sem claim exp não é considerado válido', () => {
    expect(criarServico(jwtFalso({ sub: '1' })).autenticado()).toBe(false);
  });

  it('token malformado não quebra a aplicação: é tratado como ausente', () => {
    const servico = criarServico('isso-nao-e-um-jwt');

    expect(servico.autenticado()).toBe(false);
    expect(servico.emailUsuario()).toBeNull();
  });

  it('login com "manter conectado" grava o token no localStorage', () => {
    const servico = criarServico();
    const token = jwtFalso({ sub: '1', exp: expiraEmSegundos(3600) });

    servico.login({ email: 'a@b.com', senha: '12345678' }, true).subscribe();
    const requisicao = http.expectOne(`${environment.apiUrl}/auth/login`);
    expect(requisicao.request.method).toBe('POST');
    expect(requisicao.request.body).toEqual({ email: 'a@b.com', senha: '12345678' });
    requisicao.flush({ token });

    expect(localStorage.getItem(CHAVE_TOKEN)).toBe(token);
    expect(sessionStorage.getItem(CHAVE_TOKEN)).toBeNull();
    expect(servico.token()).toBe(token);
    expect(servico.autenticado()).toBe(true);
  });

  it('login sem "manter conectado" grava o token só no sessionStorage', () => {
    const servico = criarServico();
    const token = jwtFalso({ sub: '1', exp: expiraEmSegundos(3600) });

    servico.login({ email: 'a@b.com', senha: '12345678' }, false).subscribe();
    http.expectOne(`${environment.apiUrl}/auth/login`).flush({ token });

    expect(sessionStorage.getItem(CHAVE_TOKEN)).toBe(token);
    expect(localStorage.getItem(CHAVE_TOKEN)).toBeNull();
  });

  it('logout apaga o token dos dois armazenamentos', () => {
    const servico = criarServico(jwtFalso({ sub: '1', exp: expiraEmSegundos(3600) }));
    sessionStorage.setItem(CHAVE_TOKEN, 'outro');

    servico.logout();

    expect(localStorage.getItem(CHAVE_TOKEN)).toBeNull();
    expect(sessionStorage.getItem(CHAVE_TOKEN)).toBeNull();
    expect(servico.autenticado()).toBe(false);
  });

  it('registrar faz POST em /auth/registrar', () => {
    const servico = criarServico();

    servico.registrar({ nome: 'Ana', email: 'ana@x.com', senha: '12345678' }).subscribe();
    const requisicao = http.expectOne(`${environment.apiUrl}/auth/registrar`);

    expect(requisicao.request.method).toBe('POST');
    requisicao.flush({ id: 1, nome: 'Ana', email: 'ana@x.com', criadoEm: '2026-11-25T00:00:00Z' });
  });
});
