export interface LancamentoRequest {
  acaoId: number;
  corretoraId: number;
  quantidade: number;
  precoUnitario: number;
  dataOperacao: string;
}

export interface LancamentoResponse {
  id: number;
  acaoId: number;
  tickerAcao: string;
  corretoraId: number;
  razaoSocialCorretora: string;
  quantidade: number;
  precoUnitario: number;
  dataOperacao: string;
  criadoEm: string;
}

export interface PosicaoResponse {
  acaoId: number;
  ticker: string;
  nomeEmpresa: string | null;
  quantidade: number;
  precoMedio: number;
  valorInvestido: number;
  valorAtual: number;
  variacaoPercentual: number;
}