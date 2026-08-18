export interface CorretoraRequest {
  cnpj: string;
  cep: string;
  numero?: string | null;
  complemento?: string | null;
  email?: string | null;
  telefone?: string | null;
}

export interface CorretoraResponse {
  id: number;
  cnpj: string;
  razaoSocial: string;
  nomeFantasia: string | null;
  email: string | null;
  telefone: string | null;
  cep: string;
  logradouro: string;
  numero: string | null;
  complemento: string | null;
  bairro: string;
  cidade: string;
  uf: string;
  situacaoCadastral: string;
  validadaCvm: boolean;
  criadoEm: string;
}