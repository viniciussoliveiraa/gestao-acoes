import { TestBed } from '@angular/core/testing';
import { TemaService } from './tema.service';

const CHAVE_TEMA = 'gestao-acoes.tema';

describe('TemaService (modo noturno)', () => {
  const matchMediaOriginal = window.matchMedia;

  beforeEach(() => {
    localStorage.clear();
    document.documentElement.classList.remove('tema-escuro');
    TestBed.configureTestingModule({});
  });

  afterEach(() => {
    window.matchMedia = matchMediaOriginal;
  });

  function sistemaPrefereEscuro(escuro: boolean): void {
    window.matchMedia = vi.fn().mockReturnValue({ matches: escuro }) as unknown as typeof window.matchMedia;
  }

  it('sem escolha salva segue o sistema operacional (claro)', () => {
    sistemaPrefereEscuro(false);

    expect(TestBed.inject(TemaService).tema()).toBe('claro');
  });

  it('sem escolha salva segue o sistema operacional (escuro)', () => {
    sistemaPrefereEscuro(true);

    expect(TestBed.inject(TemaService).tema()).toBe('escuro');
  });

  it('a escolha salva vence a preferência do sistema', () => {
    sistemaPrefereEscuro(false);
    localStorage.setItem(CHAVE_TEMA, 'escuro');

    expect(TestBed.inject(TemaService).escuro()).toBe(true);
  });

  it('valor salvo inválido é ignorado', () => {
    sistemaPrefereEscuro(false);
    localStorage.setItem(CHAVE_TEMA, 'roxo');

    expect(TestBed.inject(TemaService).tema()).toBe('claro');
  });

  it('alternar aplica a classe tema-escuro no <html> e salva a escolha', () => {
    sistemaPrefereEscuro(false);
    const servico = TestBed.inject(TemaService);

    servico.alternar();
    TestBed.tick();

    expect(servico.tema()).toBe('escuro');
    expect(document.documentElement.classList.contains('tema-escuro')).toBe(true);
    expect(localStorage.getItem(CHAVE_TEMA)).toBe('escuro');

    servico.alternar();
    TestBed.tick();

    expect(servico.tema()).toBe('claro');
    expect(document.documentElement.classList.contains('tema-escuro')).toBe(false);
    expect(localStorage.getItem(CHAVE_TEMA)).toBe('claro');
  });
});
