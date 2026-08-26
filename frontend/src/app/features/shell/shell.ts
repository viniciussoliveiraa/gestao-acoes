import { Component, computed, signal } from '@angular/core';
import { ActivatedRouteSnapshot, NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { filter } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';

interface PaginaAtual {
  titulo: string;
  icone: string;
}

function paginaMaisProfunda(snapshot: ActivatedRouteSnapshot): ActivatedRouteSnapshot {
  let atual = snapshot;
  while (atual.firstChild) {
    atual = atual.firstChild;
  }
  return atual;
}

@Component({
  selector: 'app-shell',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatSidenavModule,
    MatToolbarModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
  ],
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
})
export class Shell {
  protected readonly pagina = signal<PaginaAtual>({ titulo: 'Resumo', icone: 'dashboard' });

  protected readonly saudacaoNome = computed(() => {
    const email = this.authService.emailUsuario();
    return email ? email.split('@')[0] : null;
  });

  constructor(
    protected readonly authService: AuthService,
    private readonly router: Router
  ) {
    this.atualizarPagina();
    this.router.events.pipe(filter((evento) => evento instanceof NavigationEnd)).subscribe(() => {
      this.atualizarPagina();
    });
  }

  sair(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  private atualizarPagina(): void {
    const dados = paginaMaisProfunda(this.router.routerState.snapshot.root).data;
    if (dados['titulo'] && dados['icone']) {
      this.pagina.set({ titulo: dados['titulo'] as string, icone: dados['icone'] as string });
    }
  }
}
