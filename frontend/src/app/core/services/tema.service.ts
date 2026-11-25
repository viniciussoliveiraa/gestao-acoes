import { Injectable, effect, signal } from '@angular/core';

export type Tema = 'claro' | 'escuro';

/** Mesma chave lida pelo script inline do index.html (aplica o tema antes do Angular subir). */
const CHAVE_TEMA = 'gestao-acoes.tema';
const CLASSE_ESCURO = 'tema-escuro';

/**
 * Modo claro/escuro da interface. A escolha fica só neste navegador (localStorage); sem escolha
 * salva, segue a preferência do sistema operacional. O tema é aplicado pela classe `tema-escuro`
 * no <html>, que troca as variáveis --app-* e o color-scheme do Angular Material (styles.scss).
 */
@Injectable({ providedIn: 'root' })
export class TemaService {
  readonly tema = signal<Tema>(this.temaInicial());

  constructor() {
    effect(() => {
      const tema = this.tema();
      document.documentElement.classList.toggle(CLASSE_ESCURO, tema === 'escuro');
      try {
        localStorage.setItem(CHAVE_TEMA, tema);
      } catch {
        // Navegação privada / armazenamento bloqueado: o tema vale só para esta sessão.
      }
    });
  }

  escuro(): boolean {
    return this.tema() === 'escuro';
  }

  alternar(): void {
    this.tema.update((atual) => (atual === 'escuro' ? 'claro' : 'escuro'));
  }

  private temaInicial(): Tema {
    try {
      const salvo = localStorage.getItem(CHAVE_TEMA);
      if (salvo === 'claro' || salvo === 'escuro') {
        return salvo;
      }
    } catch {
      // Sem acesso ao armazenamento: cai na preferência do sistema.
    }
    return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'escuro' : 'claro';
  }
}
