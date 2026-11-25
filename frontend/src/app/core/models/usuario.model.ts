export type Papel = 'ADMIN' | 'USER';

/** Usuário como devolvido por GET /usuarios (gerenciamento administrativo, Aula 14). */
export interface Usuario {
  id: number;
  nome: string;
  email: string;
  papel: Papel;
  ativo: boolean;
  criadoEm: string;
}

/** Corpo de PUT /usuarios/{id}: o email é o login do usuário. */
export interface UsuarioUpdateRequest {
  nome: string;
  email: string;
  papel: Papel;
  ativo: boolean;
}

/** Corpo de PUT /usuarios/{id}/senha. */
export interface AlterarSenhaRequest {
  senha: string;
}
