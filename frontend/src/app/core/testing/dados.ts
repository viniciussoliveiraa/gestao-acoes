import { HttpErrorResponse } from '@angular/common/http';
import { throwError } from 'rxjs';
import { AcaoResponse } from '../models/acao.model';
import { LancamentoResponse, PosicaoResponse } from '../models/carteira.model';
import { CorretoraResponse } from '../models/corretora.model';
import { Pagina } from '../models/pagina.model';
import { ProventoResponse } from '../models/provento.model';
import { Usuario } from '../models/usuario.model';

/** Dados de exemplo para os testes de tela (só os campos variam conforme o caso). */

export function pagina<T>(content: T[]): Pagina<T> {
  return { content, totalElements: content.length, totalPages: 1, number: 0, size: content.length };
}

export function acao(dados: Partial<AcaoResponse> = {}): AcaoResponse {
  return {
    id: 1,
    ticker: 'PETR4',
    nomeEmpresa: 'Petrobras',
    mercado: 'BRASIL',
    moeda: 'BRL',
    cotacaoAtual: 38.5,
    dataHoraCotacao: '2026-11-24T18:00:00Z',
    provedorOrigem: 'brapi',
    criadoEm: '2026-11-01T00:00:00Z',
    ...dados,
  };
}

export function corretora(dados: Partial<CorretoraResponse> = {}): CorretoraResponse {
  return {
    id: 1,
    cnpj: '02332886000104',
    razaoSocial: 'XP Investimentos',
    nomeFantasia: 'XP',
    email: null,
    telefone: null,
    cep: '04538133',
    logradouro: 'Av. Juscelino Kubitschek',
    numero: '1909',
    complemento: null,
    bairro: 'Itaim Bibi',
    cidade: 'São Paulo',
    uf: 'SP',
    situacaoCadastral: 'ATIVA',
    validadaCvm: true,
    criadoEm: '2026-11-01T00:00:00Z',
    ...dados,
  };
}

export function lancamento(dados: Partial<LancamentoResponse> = {}): LancamentoResponse {
  return {
    id: 1,
    acaoId: 1,
    tickerAcao: 'PETR4',
    corretoraId: 1,
    razaoSocialCorretora: 'XP Investimentos',
    tipo: 'COMPRA',
    quantidade: 10,
    precoUnitario: 30,
    dataOperacao: '2026-11-10',
    criadoEm: '2026-11-10T00:00:00Z',
    ...dados,
  };
}

export function posicao(dados: Partial<PosicaoResponse> = {}): PosicaoResponse {
  return {
    acaoId: 1,
    ticker: 'PETR4',
    nomeEmpresa: 'Petrobras',
    quantidade: 10,
    precoMedio: 30,
    valorInvestido: 300,
    valorAtual: 385,
    variacaoPercentual: 28.33,
    resultadoRealizado: 0,
    ...dados,
  };
}

export function provento(dados: Partial<ProventoResponse> = {}): ProventoResponse {
  return {
    id: 1,
    acaoId: 1,
    tickerAcao: 'PETR4',
    tipo: 'DIVIDENDO',
    valorTotal: 12.5,
    dataPagamento: '2026-11-15',
    criadoEm: '2026-11-15T00:00:00Z',
    ...dados,
  };
}

export function usuario(dados: Partial<Usuario> = {}): Usuario {
  return {
    id: 2,
    nome: 'Ana',
    email: 'ana@exemplo.com',
    papel: 'USER',
    ativo: true,
    criadoEm: '2026-11-01T00:00:00Z',
    ...dados,
  };
}

/** Simula uma resposta de erro do backend no formato Problem Details. */
export function erroHttp(status: number, detail = 'Erro do backend') {
  return throwError(() => new HttpErrorResponse({ status, error: { detail } }));
}

/** Liga/desliga a preferência "menos animação" do sistema (afeta Resumo e a tela de login). */
export function simularMenosMovimento(ativo: boolean): void {
  window.matchMedia = ((query: string) => ({
    matches: ativo && query.includes('reduced-motion'),
    media: query,
    addEventListener: () => undefined,
    removeEventListener: () => undefined,
  })) as unknown as typeof window.matchMedia;
}
