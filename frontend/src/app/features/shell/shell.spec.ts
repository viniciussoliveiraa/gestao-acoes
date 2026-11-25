import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { TemaService } from '../../core/services/tema.service';
import { Shell } from './shell';

@Component({ template: '' })
class TelaVazia {}

describe('Shell (menu lateral e barra do topo)', () => {
  let auth: {
    autenticado: ReturnType<typeof vi.fn>;
    administrador: ReturnType<typeof vi.fn>;
    emailUsuario: ReturnType<typeof vi.fn>;
    logout: ReturnType<typeof vi.fn>;
  };

  function criar(opcoes: { autenticado: boolean; administrador?: boolean; email?: string | null }) {
    localStorage.clear();
    auth = {
      autenticado: vi.fn(() => opcoes.autenticado),
      administrador: vi.fn(() => opcoes.administrador ?? false),
      emailUsuario: vi.fn(() => opcoes.email ?? null),
      logout: vi.fn(),
    };
    TestBed.configureTestingModule({
      imports: [Shell],
      providers: [
        provideRouter([
          { path: 'login', component: TelaVazia },
          { path: 'graficos', component: TelaVazia, data: { titulo: 'Gráficos', icone: 'pie_chart' } },
          { path: 'sem-dados', component: TelaVazia },
        ]),
        { provide: AuthService, useValue: auth },
      ],
    });
    const fixture = TestBed.createComponent(Shell);
    fixture.detectChanges();
    return fixture;
  }

  it('mostra o nome (parte antes do @) e o botão Sair quando logado', () => {
    const fixture = criar({ autenticado: true, email: 'vinicius@exemplo.com' });
    const texto = fixture.nativeElement.textContent as string;

    expect(texto).toContain('vinicius');
    expect(texto).toContain('Sair');
    expect(texto).not.toContain('Entrar');
  });

  it('logado sem email no token não mostra o nome', () => {
    const fixture = criar({ autenticado: true, email: null });

    expect(fixture.nativeElement.querySelector('.shell__user')).toBeNull();
  });

  it('sem login mostra o botão Entrar', () => {
    const fixture = criar({ autenticado: false });

    expect(fixture.nativeElement.textContent).toContain('Entrar');
  });

  it('o link Usuários só aparece para administrador', () => {
    expect(criar({ autenticado: true, administrador: false }).nativeElement.textContent).not.toContain('Usuários');
    TestBed.resetTestingModule();
    expect(criar({ autenticado: true, administrador: true }).nativeElement.textContent).toContain('Usuários');
  });

  it('o título da barra acompanha a tela aberta', async () => {
    const fixture = criar({ autenticado: true });

    await TestBed.inject(Router).navigateByUrl('/graficos');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.shell__toolbar-title').textContent).toContain('Gráficos');

    // Tela sem título configurado mantém o título anterior.
    await TestBed.inject(Router).navigateByUrl('/sem-dados');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.shell__toolbar-title').textContent).toContain('Gráficos');
  });

  it('o botão de lua/sol alterna o modo noturno', () => {
    const fixture = criar({ autenticado: true });
    const tema = TestBed.inject(TemaService);
    const antes = tema.tema();

    fixture.nativeElement.querySelector('.shell__tema').click();
    fixture.detectChanges();

    expect(tema.tema()).not.toBe(antes);
  });

  it('Sair faz logout e volta para /login', async () => {
    const fixture = criar({ autenticado: true });
    const router = TestBed.inject(Router);
    const navegar = vi.spyOn(router, 'navigate');

    fixture.componentInstance.sair();

    expect(auth.logout).toHaveBeenCalled();
    expect(navegar).toHaveBeenCalledWith(['/login']);
  });
});
