export interface RegistrarUsuarioRequest {
  nome: string;
  email: string;
  senha: string;
}

export interface LoginRequest {
  email: string;
  senha: string;
}

export interface LoginResponse {
  token: string;
}

export interface UsuarioResponse {
  id: number;
  nome: string;
  email: string;
  criadoEm: string;
}