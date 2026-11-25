import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { UsuarioService } from './usuario.service';

describe('UsuarioService', () => {
  let servico: UsuarioService;
  let http: HttpTestingController;
  const base = `${environment.apiUrl}/usuarios`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    servico = TestBed.inject(UsuarioService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('listar faz GET paginado e ordenado por nome', () => {
    servico.listar().subscribe();

    const requisicao = http.expectOne((r) => r.url === base);
    expect(requisicao.request.method).toBe('GET');
    expect(requisicao.request.params.get('page')).toBe('0');
    expect(requisicao.request.params.get('size')).toBe('100');
    expect(requisicao.request.params.get('sort')).toBe('nome');
    requisicao.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 100 });
  });

  it('atualizar faz PUT em /usuarios/{id} com login, nome, papel e situação', () => {
    const corpo = { nome: 'Ana', email: 'novo@x.com', papel: 'USER' as const, ativo: true };

    servico.atualizar(7, corpo).subscribe();

    const requisicao = http.expectOne(`${base}/7`);
    expect(requisicao.request.method).toBe('PUT');
    expect(requisicao.request.body).toEqual(corpo);
    requisicao.flush({ id: 7, ...corpo, criadoEm: '2026-11-25T00:00:00Z' });
  });

  it('alterarSenha faz PUT em /usuarios/{id}/senha só com a senha', () => {
    servico.alterarSenha(7, { senha: 'senhaNova123' }).subscribe();

    const requisicao = http.expectOne(`${base}/7/senha`);
    expect(requisicao.request.method).toBe('PUT');
    expect(requisicao.request.body).toEqual({ senha: 'senhaNova123' });
    requisicao.flush(null, { status: 204, statusText: 'No Content' });
  });
});
