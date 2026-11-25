import { TestBed } from '@angular/core/testing';
import { LOCALE_ID } from '@angular/core';
import { provideRouter } from '@angular/router';
import { App } from './app';
import { appConfig } from './app.config';
import { routes } from './app.routes';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([])],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });
});

describe('Configuração da aplicação', () => {
  it('usa o locale pt-BR (moeda R$ 1.234,56)', () => {
    const locale = appConfig.providers.find(
      (p) => typeof p === 'object' && p !== null && 'provide' in p && p.provide === LOCALE_ID
    ) as { useValue: string } | undefined;

    expect(locale?.useValue).toBe('pt-BR');
  });
});

describe('Rotas', () => {
  const filhas = routes.find((r) => r.path === '')?.children ?? [];

  it('raiz redireciona para /resumo e rota desconhecida volta para a raiz', () => {
    expect(filhas.find((r) => r.path === '')?.redirectTo).toBe('resumo');
    expect(routes.find((r) => r.path === '**')?.redirectTo).toBe('');
  });

  it('telas da carteira exigem login (authGuard)', () => {
    for (const caminho of ['resumo', 'lancamentos', 'proventos', 'graficos', 'rebalanceamento']) {
      expect(filhas.find((r) => r.path === caminho)?.canActivate?.length, caminho).toBe(1);
    }
  });

  it('Corretoras e Ações são públicas; Usuários exige admin', () => {
    expect(filhas.find((r) => r.path === 'corretoras')?.canActivate).toBeUndefined();
    expect(filhas.find((r) => r.path === 'acoes')?.canActivate).toBeUndefined();
    expect(filhas.find((r) => r.path === 'usuarios')?.canActivate?.length).toBe(1);
  });

  it('toda tela carregada sob demanda (lazy) resolve para um componente', async () => {
    const lazy = filhas.filter((r) => r.loadComponent);
    expect(lazy.length).toBe(8);
    for (const rota of lazy) {
      const componente = await rota.loadComponent!();
      expect(componente, rota.path).toBeTruthy();
    }
  });
});
