export type Mercado = 'BRASIL' | 'ESTADOS_UNIDOS';
export type Moeda = 'BRL' | 'USD';

export interface AcaoRequest {
  ticker: string;
  mercado: Mercado;
}

export interface AcaoResponse {
  id: number;
  ticker: string;
  nomeEmpresa: string | null;
  mercado: Mercado;
  moeda: Moeda;
  cotacaoAtual: number;
  dataHoraCotacao: string;
  provedorOrigem: string;
  criadoEm: string;
}