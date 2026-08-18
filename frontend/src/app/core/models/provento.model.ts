export type TipoProvento = 'DIVIDENDO' | 'JCP';

export interface ProventoRequest {
  acaoId: number;
  tipo: TipoProvento;
  valorTotal: number;
  dataPagamento: string;
}

export interface ProventoResponse {
  id: number;
  acaoId: number;
  tickerAcao: string;
  tipo: TipoProvento;
  valorTotal: number;
  dataPagamento: string;
  criadoEm: string;
}