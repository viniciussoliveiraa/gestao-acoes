import { HttpErrorResponse } from '@angular/common/http';
import { ProblemDetails } from '../models/problem-details.model';

/** Extrai uma mensagem amigável de um erro HTTP no formato Problem Details (RFC 9457). */
export function mensagemDeErro(erro: unknown): string {
  if (erro instanceof HttpErrorResponse) {
    const problem = erro.error as ProblemDetails | undefined;
    if (problem?.errors?.length) {
      return problem.errors.map((e) => e.mensagem).join('; ');
    }
    if (problem?.detail) {
      return problem.detail;
    }
    if (erro.status === 0) {
      return 'Não foi possível conectar à API. Verifique se o backend está rodando.';
    }
  }
  return 'Ocorreu um erro inesperado. Tente novamente.';
}