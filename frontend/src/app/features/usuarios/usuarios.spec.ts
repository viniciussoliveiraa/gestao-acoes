import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { UsuarioService } from '../../core/services/usuario.service';
import { erroHttp, pagina, usuario } from '../../core/testing/dados';
import { Usuarios } from './usuarios';

/* eslint-disable @typescript-eslint/no-explicit-any */

describe('Tela Usuários (administração)', () => {
  let servico: {
    listar: ReturnType<typeof vi.fn>;
    atualizar: ReturnType<typeof vi.fn>;
    alterarSenha: ReturnType<typeof vi.fn>;
  };
  const admin = usuario({ id: 10, nome: 'Admin', email: 'admin@gestao-acoes.local', papel: 'ADMIN' });
  const ana = usuario({ id: 2, nome: 'Ana', email: 'ana@exemplo.com' });

  function criar(lista = pagina([admin, ana])) {
    servico = {
      listar: vi.fn(() => of(lista)),
      atualizar: vi.fn((id: number, corpo: object) => of({ ...ana, ...corpo, id })),
      alterarSenha: vi.fn(() => of(undefined)),
    };
    TestBed.configureTestingModule({
      imports: [Usuarios],
      providers: [
        { provide: UsuarioService, useValue: servico },
        { provide: AuthService, useValue: { idUsuario: () => 10 } },
      ],
    });
    const fixture = TestBed.createComponent(Usuarios);
    fixture.detectChanges();
    return { fixture, tela: fixture.componentInstance as any, texto: () => fixture.nativeElement.textContent as string };
  }

  it('lista os usuários e marca o admin logado como "você"', () => {
    const { texto, tela } = criar();

    expect(texto()).toContain('ana@exemplo.com');
    expect(texto()).toContain('você');
    expect(tela.ehVoceMesmo(admin)).toBe(true);
    expect(tela.ehVoceMesmo(ana)).toBe(false);
  });

  it('lista vazia mostra a mensagem', () => {
    expect(criar(pagina([])).texto()).toContain('Nenhum usuário cadastrado');
  });

  it('erro ao listar (ex.: 403) aparece no lugar da tabela', () => {
    TestBed.configureTestingModule({
      imports: [Usuarios],
      providers: [
        { provide: UsuarioService, useValue: { listar: () => erroHttp(403, 'Acesso negado') } },
        { provide: AuthService, useValue: { idUsuario: () => 10 } },
      ],
    });
    const fixture = TestBed.createComponent(Usuarios);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Acesso negado');
  });

  it('Editar abre os formulários preenchidos; Fechar fecha', () => {
    const { fixture, tela, texto } = criar();

    tela.editar(ana);
    fixture.detectChanges();
    expect(tela.formDados.getRawValue()).toEqual({ nome: 'Ana', email: 'ana@exemplo.com', papel: 'USER', ativo: true });
    expect(texto()).toContain('Nova senha');

    tela.cancelar();
    fixture.detectChanges();
    expect(tela.selecionado()).toBeNull();
  });

  it('salvar sem ninguém selecionado não faz nada', () => {
    const { tela } = criar();

    tela.salvarDados();
    tela.salvarSenha();

    expect(servico.atualizar).not.toHaveBeenCalled();
    expect(servico.alterarSenha).not.toHaveBeenCalled();
  });

  it('dados inválidos (email errado) não chamam a API', () => {
    const { tela } = criar();
    tela.editar(ana);
    tela.formDados.controls.email.setValue('nao-e-email');

    tela.salvarDados();

    expect(servico.atualizar).not.toHaveBeenCalled();
  });

  it('salvar sem trocar o email só confirma os dados', () => {
    const { tela } = criar();
    tela.editar(ana);
    tela.formDados.controls.nome.setValue('Ana Maria');

    tela.salvarDados();

    expect(servico.atualizar).toHaveBeenCalledWith(2, expect.objectContaining({ nome: 'Ana Maria' }));
    expect(tela.sucessoDados()).toBe('Dados de "Ana Maria" atualizados.');
  });

  it('trocar o login de outro usuário avisa o novo login e atualiza a tabela', () => {
    const { tela } = criar();
    tela.editar(ana);
    tela.formDados.controls.email.setValue('ana.nova@exemplo.com');

    tela.salvarDados();

    expect(tela.sucessoDados()).toContain('O login agora é ana.nova@exemplo.com.');
    expect(tela.sucessoDados()).not.toContain('próximo login');
    expect(tela.usuarios().find((u: { id: number }) => u.id === 2).email).toBe('ana.nova@exemplo.com');
  });

  it('trocar o próprio login avisa para usar o novo email no próximo login', () => {
    const { tela } = criar();
    tela.editar(admin);
    tela.formDados.controls.email.setValue('novo.admin@exemplo.com');

    tela.salvarDados();

    expect(tela.sucessoDados()).toContain('Use o novo email no próximo login.');
  });

  it('erro ao salvar dados (ex.: último admin, 422) aparece na tela', () => {
    const { tela } = criar();
    servico.atualizar.mockReturnValue(erroHttp(422, 'Não é possível rebaixar ou desativar o último administrador ativo'));
    tela.editar(admin);
    tela.formDados.controls.papel.setValue('USER');

    tela.salvarDados();

    expect(tela.erroDados()).toContain('último administrador');
    expect(tela.salvandoDados()).toBe(false);
  });

  it('senha curta ou confirmação diferente não chamam a API', () => {
    const { fixture, tela, texto } = criar();
    tela.editar(ana);

    tela.formSenha.setValue({ senha: '123', confirmacao: '123' });
    tela.salvarSenha();
    tela.formSenha.setValue({ senha: 'senhaNova123', confirmacao: 'outraSenha1' });
    tela.salvarSenha();
    fixture.detectChanges();

    expect(servico.alterarSenha).not.toHaveBeenCalled();
    expect(texto()).toContain('As senhas não conferem.');
  });

  it('nova senha válida é enviada e o formulário é limpo', () => {
    const { tela } = criar();
    tela.editar(ana);
    tela.formSenha.setValue({ senha: 'senhaNova123', confirmacao: 'senhaNova123' });

    tela.salvarSenha();

    expect(servico.alterarSenha).toHaveBeenCalledWith(2, { senha: 'senhaNova123' });
    expect(tela.sucessoSenha()).toBe('Senha de "Ana" alterada.');
    expect(tela.formSenha.controls.senha.value).toBe('');
  });

  it('erro ao trocar a senha aparece na tela', () => {
    const { tela } = criar();
    servico.alterarSenha.mockReturnValue(erroHttp(400, 'senha deve ter entre 8 e 72 caracteres'));
    tela.editar(ana);
    tela.formSenha.setValue({ senha: 'senhaNova123', confirmacao: 'senhaNova123' });

    tela.salvarSenha();

    expect(tela.erroSenha()).toBe('senha deve ter entre 8 e 72 caracteres');
    expect(tela.salvandoSenha()).toBe(false);
  });
});
