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
/** Prévia dos dados do CNPJ (GET /corretoras/consulta-cnpj/{cnpj}), usada para preencher o formulário. */
export interface ConsultaCnpj {
  cnpj: string;
  razaoSocial: string;
  nomeFantasia: string | null;
  situacaoCadastral: string | null;
  email: string | null;
  telefone: string | null;
  cep: string | null;
  logradouro: string | null;
  numero: string | null;
  complemento: string | null;
  bairro: string | null;
  cidade: string | null;
  uf: string | null;
  /** false = não consta como em funcionamento normal na CVM: o cadastro seria recusado (422). */
  validadaCvm: boolean;
}
