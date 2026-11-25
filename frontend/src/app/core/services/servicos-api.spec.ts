import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { AcaoRequest } from '../models/acao.model';
import { LancamentoRequest } from '../models/carteira.model';
import { CorretoraRequest } from '../models/corretora.model';
import { ProventoRequest } from '../models/provento.model';
import { AcaoService } from './acao.service';
import { CarteiraService } from './carteira.service';
import { CorretoraService } from './corretora.service';
import { ProventoService } from './provento.service';

/**
 * Serviços que só repassam chamadas para a API: o teste confere método HTTP, URL, parâmetros
 * de paginação e corpo — ou seja, que o front conversa com o backend no contrato certo.
 */
describe('Serviços HTTP da API', () => {
  let http: HttpTestingController;
  const api = environment.apiUrl;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function esperar(metodo: string, url: string, params?: Record<string, string>) {
    const requisicao = http.expectOne((r) => r.method === metodo && r.url === url);
    for (const [chave, valor] of Object.entries(params ?? {})) {
      expect(requisicao.request.params.get(chave)).toBe(valor);
    }
    return requisicao;
  }

  describe('AcaoService', () => {
    let servico: AcaoService;
    beforeEach(() => (servico = TestBed.inject(AcaoService)));

    it('cadastrar faz POST em /acoes', () => {
      const corpo = { ticker: 'PETR4', mercado: 'BRASIL' } as AcaoRequest;
      servico.cadastrar(corpo).subscribe();
      expect(esperar('POST', `${api}/acoes`).request.body).toEqual(corpo);
    });

    it('listar usa página 0 e tamanho 20 por padrão', () => {
      servico.listar().subscribe();
      esperar('GET', `${api}/acoes`, { page: '0', size: '20' }).flush({ content: [] });
    });

    it('listar repassa página e tamanho informados', () => {
      servico.listar(2, 50).subscribe();
      esperar('GET', `${api}/acoes`, { page: '2', size: '50' }).flush({ content: [] });
    });

    it('buscarPorTicker faz GET em /acoes/ticker/{ticker}', () => {
      servico.buscarPorTicker('AAPL').subscribe();
      esperar('GET', `${api}/acoes/ticker/AAPL`).flush({});
    });

    it('atualizarCotacao faz PUT em /acoes/{id}/atualizar-cotacao', () => {
      servico.atualizarCotacao(3).subscribe();
      esperar('PUT', `${api}/acoes/3/atualizar-cotacao`).flush({});
    });
  });

  describe('CarteiraService', () => {
    let servico: CarteiraService;
    beforeEach(() => (servico = TestBed.inject(CarteiraService)));

    it('registrarLancamento faz POST em /carteira/lancamentos', () => {
      const corpo = { tipo: 'COMPRA', acaoId: 1, corretoraId: 1, quantidade: 10 } as unknown as LancamentoRequest;
      servico.registrarLancamento(corpo).subscribe();
      expect(esperar('POST', `${api}/carteira/lancamentos`).request.body).toEqual(corpo);
    });

    it('listarLancamentos usa página 0 e tamanho 20 por padrão', () => {
      servico.listarLancamentos().subscribe();
      esperar('GET', `${api}/carteira/lancamentos`, { page: '0', size: '20' }).flush({ content: [] });
    });

    it('listarLancamentos repassa página e tamanho informados', () => {
      servico.listarLancamentos(0, 1000).subscribe();
      esperar('GET', `${api}/carteira/lancamentos`, { page: '0', size: '1000' }).flush({ content: [] });
    });

    it('listarPosicoes faz GET em /carteira/posicoes', () => {
      servico.listarPosicoes().subscribe();
      esperar('GET', `${api}/carteira/posicoes`).flush([]);
    });

    it('excluirLancamento faz DELETE em /carteira/lancamentos/{id}', () => {
      servico.excluirLancamento(9).subscribe();
      esperar('DELETE', `${api}/carteira/lancamentos/9`).flush(null);
    });
  });

  describe('CorretoraService', () => {
    let servico: CorretoraService;
    beforeEach(() => (servico = TestBed.inject(CorretoraService)));

    it('cadastrar faz POST em /corretoras', () => {
      const corpo = { cnpj: '02332886000104', cep: '04538133' } as CorretoraRequest;
      servico.cadastrar(corpo).subscribe();
      expect(esperar('POST', `${api}/corretoras`).request.body).toEqual(corpo);
    });

    it('listar usa página 0 e tamanho 20 por padrão', () => {
      servico.listar().subscribe();
      esperar('GET', `${api}/corretoras`, { page: '0', size: '20' }).flush({ content: [] });
    });

    it('listar repassa página e tamanho informados', () => {
      servico.listar(1, 5).subscribe();
      esperar('GET', `${api}/corretoras`, { page: '1', size: '5' }).flush({ content: [] });
    });

    it('buscarPorCnpj faz GET em /corretoras/cnpj/{cnpj}', () => {
      servico.buscarPorCnpj('02332886000104').subscribe();
      esperar('GET', `${api}/corretoras/cnpj/02332886000104`).flush({});
    });
  });

  describe('ProventoService', () => {
    let servico: ProventoService;
    beforeEach(() => (servico = TestBed.inject(ProventoService)));

    it('registrar faz POST em /proventos', () => {
      const corpo = { acaoId: 1, tipo: 'DIVIDENDO' } as unknown as ProventoRequest;
      servico.registrar(corpo).subscribe();
      expect(esperar('POST', `${api}/proventos`).request.body).toEqual(corpo);
    });

    it('listar usa página 0 e tamanho 20 por padrão', () => {
      servico.listar().subscribe();
      esperar('GET', `${api}/proventos`, { page: '0', size: '20' }).flush({ content: [] });
    });

    it('listar repassa página e tamanho informados', () => {
      servico.listar(3, 10).subscribe();
      esperar('GET', `${api}/proventos`, { page: '3', size: '10' }).flush({ content: [] });
    });
  });
});
