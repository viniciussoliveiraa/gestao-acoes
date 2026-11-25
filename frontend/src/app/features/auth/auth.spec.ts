import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { erroHttp, simularMenosMovimento } from '../../core/testing/dados';
import { AuthHero } from './auth-hero/auth-hero';
import { Login } from './login/login';
import { Registro } from './registro/registro';

/* eslint-disable @typescript-eslint/no-explicit-any */

describe('Telas de autenticação', () => {
  let auth: { login: ReturnType<typeof vi.fn>; registrar: ReturnType<typeof vi.fn> };
  const matchMediaOriginal = window.matchMedia;

  function configurar(): void {
    auth = { login: vi.fn(() => of({ token: 't' })), registrar: vi.fn(() => of({})) };
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthService, useValue: auth }],
    });
  }

  beforeEach(() => {
    simularMenosMovimento(true);
  });

  afterEach(() => {
    vi.useRealTimers();
    window.matchMedia = matchMediaOriginal;
  });

  describe('Login', () => {
    function criar() {
      configurar();
      const fixture = TestBed.createComponent(Login);
      fixture.detectChanges();
      return { fixture, tela: fixture.componentInstance as any, navegar: vi.spyOn(TestBed.inject(Router), 'navigate') };
    }

    it('formulário vazio não chama a API e marca os campos', () => {
      const { tela } = criar();

      tela.submeter();

      expect(auth.login).not.toHaveBeenCalled();
      expect(tela.form.touched).toBe(true);
    });

    it('login certo mostra sucesso e vai para /resumo depois de um instante', () => {
      vi.useFakeTimers();
      const { fixture, tela, navegar } = criar();
      tela.form.setValue({ email: 'ana@exemplo.com', senha: '12345678', lembrar: false });

      tela.submeter();
      fixture.detectChanges();

      expect(auth.login).toHaveBeenCalledWith({ email: 'ana@exemplo.com', senha: '12345678' }, false);
      expect(tela.sucesso()).toBe(true);
      expect(navegar).not.toHaveBeenCalled();
      vi.advanceTimersByTime(600);
      expect(navegar).toHaveBeenCalledWith(['/resumo']);
    });

    it('senha errada mostra a mensagem do backend', () => {
      const { fixture, tela } = criar();
      auth.login.mockReturnValue(erroHttp(401, 'Email ou senha inválidos'));
      tela.form.setValue({ email: 'ana@exemplo.com', senha: 'errada123', lembrar: true });

      tela.submeter();
      fixture.detectChanges();

      expect(tela.carregando()).toBe(false);
      expect(fixture.nativeElement.textContent).toContain('Email ou senha inválidos');
    });

    it.each([
      [8, 'Bom dia'],
      [14, 'Boa tarde'],
      [21, 'Boa noite'],
    ])('às %ih a saudação é "%s"', (hora, saudacao) => {
      vi.useFakeTimers();
      vi.setSystemTime(new Date(2026, 10, 25, hora, 0, 0));

      expect(criar().tela.saudacao).toBe(saudacao);
    });
  });

  describe('Registro', () => {
    function criar() {
      configurar();
      const fixture = TestBed.createComponent(Registro);
      fixture.detectChanges();
      return { fixture, tela: fixture.componentInstance as any, navegar: vi.spyOn(TestBed.inject(Router), 'navigate') };
    }

    it('formulário inválido não chama a API', () => {
      const { tela } = criar();

      tela.submeter();

      expect(auth.registrar).not.toHaveBeenCalled();
    });

    it('cadastro certo faz login automático e vai para /resumo', () => {
      vi.useFakeTimers();
      const { tela, navegar } = criar();
      tela.form.setValue({ nome: 'Ana', email: 'ana@exemplo.com', senha: 'Senha@Forte123' });

      tela.submeter();
      vi.advanceTimersByTime(600);

      expect(auth.registrar).toHaveBeenCalledWith({ nome: 'Ana', email: 'ana@exemplo.com', senha: 'Senha@Forte123' });
      expect(auth.login).toHaveBeenCalledWith({ email: 'ana@exemplo.com', senha: 'Senha@Forte123' });
      expect(tela.sucesso()).toBe(true);
      expect(navegar).toHaveBeenCalledWith(['/resumo']);
    });

    it('email já cadastrado mostra o erro do backend', () => {
      const { tela } = criar();
      auth.registrar.mockReturnValue(erroHttp(409, 'Já existe um usuário cadastrado com este email'));
      tela.form.setValue({ nome: 'Ana', email: 'ana@exemplo.com', senha: '12345678' });

      tela.submeter();

      expect(tela.erro()).toBe('Já existe um usuário cadastrado com este email');
      expect(tela.carregando()).toBe(false);
    });

    it('se o cadastro passa mas o login automático falha, mostra o erro', () => {
      const { tela } = criar();
      auth.login.mockReturnValue(erroHttp(500, 'Falha no login'));
      tela.form.setValue({ nome: 'Ana', email: 'ana@exemplo.com', senha: '12345678' });

      tela.submeter();

      expect(tela.erro()).toBe('Falha no login');
      expect(tela.sucesso()).toBe(false);
    });

    it.each([
      ['', null],
      ['abc', 'Muito fraca'],
      ['abcdefgh', 'Fraca'],
      ['abcdefgH', 'Razoável'],
      ['abcdefgH1', 'Forte'],
      ['abcdefgH1!', 'Muito forte'],
      ['abcdefgH1!xyz', 'Muito forte'],
    ])('força da senha "%s" = %s', (senha, rotulo) => {
      const { tela } = criar();

      tela.senhaDigitada.set(senha);

      expect(tela.forcaSenha()?.rotulo ?? null).toBe(rotulo);
    });
  });

  describe('AuthHero (painel decorativo)', () => {
    function criar() {
      TestBed.configureTestingModule({});
      const fixture = TestBed.createComponent(AuthHero);
      fixture.componentRef.setInput('titulo', 'Título');
      fixture.componentRef.setInput('subtitulo', 'Subtítulo');
      fixture.detectChanges();
      return { fixture, hero: fixture.componentInstance as any };
    }

    it('com "menos animação" já mostra o valor final, sem timer nem parallax', () => {
      simularMenosMovimento(true);
      const { fixture, hero } = criar();

      expect(hero.valorGerido()).toBe(128450);
      expect(hero.valorGeridoFormatado()).toBe('128.450');
      hero.aoMoverMouse(new MouseEvent('mousemove', { clientX: 10, clientY: 10 }));
      expect(hero.parallax()).toEqual({ x: 0, y: 0 });
      fixture.destroy();
    });

    it('com animação: conta o valor, troca a notificação e segue o mouse', () => {
      simularMenosMovimento(false);
      vi.useFakeTimers();
      const { fixture, hero } = criar();
      const primeira = hero.notificacaoAtual();

      vi.advanceTimersByTime(1600);
      expect(hero.valorGerido()).toBe(128450);

      vi.advanceTimersByTime(3400);
      expect(hero.notificacaoAtual()).not.toEqual(primeira);

      // jsdom não calcula layout (largura 0): simula um painel de 200x100.
      fixture.nativeElement.getBoundingClientRect = () => ({ left: 0, top: 0, width: 200, height: 100 });
      hero.aoMoverMouse(new MouseEvent('mousemove', { clientX: 50, clientY: 25 }));
      expect(hero.parallax().x).toBeLessThan(0);
      expect(hero.spotlight()).toEqual({ x: 25, y: 25 });

      fixture.destroy();
    });
  });
});
