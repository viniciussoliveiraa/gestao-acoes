import { HttpErrorResponse } from '@angular/common/http';
import { mensagemDeErro } from './erro.util';

describe('mensagemDeErro', () => {
  it('junta as mensagens de validação por campo (400 do backend)', () => {
    const erro = new HttpErrorResponse({
      status: 400,
      error: {
        errors: [
          { campo: 'email', mensagem: 'email inválido' },
          { campo: 'senha', mensagem: 'senha deve ter entre 8 e 72 caracteres' },
        ],
      },
    });

    expect(mensagemDeErro(erro)).toBe('email inválido; senha deve ter entre 8 e 72 caracteres');
  });

  it('usa o detail do Problem Details quando não há erros por campo', () => {
    const erro = new HttpErrorResponse({
      status: 422,
      error: { detail: 'Não é possível rebaixar ou desativar o último administrador ativo' },
    });

    expect(mensagemDeErro(erro)).toBe('Não é possível rebaixar ou desativar o último administrador ativo');
  });

  it('status 0 significa backend fora do ar', () => {
    expect(mensagemDeErro(new HttpErrorResponse({ status: 0 }))).toBe(
      'Não foi possível conectar à API. Verifique se o backend está rodando.'
    );
  });

  it('erro HTTP sem corpo conhecido cai na mensagem genérica', () => {
    expect(mensagemDeErro(new HttpErrorResponse({ status: 500 }))).toBe(
      'Ocorreu um erro inesperado. Tente novamente.'
    );
  });

  it('erro que não é HTTP também cai na mensagem genérica', () => {
    expect(mensagemDeErro(new Error('qualquer'))).toBe('Ocorreu um erro inesperado. Tente novamente.');
  });
});
