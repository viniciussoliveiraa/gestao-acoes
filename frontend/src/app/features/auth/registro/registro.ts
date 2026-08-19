import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../../core/services/auth.service';
import { mensagemDeErro } from '../../../core/services/erro.util';
import { AuthHero } from '../auth-hero/auth-hero';

interface ForcaSenha {
  percentual: number;
  rotulo: string;
  cor: string;
}

const NIVEIS_FORCA: ForcaSenha[] = [
  { percentual: 20, rotulo: 'Muito fraca', cor: '#cb0b38' },
  { percentual: 40, rotulo: 'Fraca', cor: '#e0793a' },
  { percentual: 60, rotulo: 'Razoável', cor: '#d3b583' },
  { percentual: 80, rotulo: 'Forte', cor: '#8a7454' },
  { percentual: 100, rotulo: 'Muito forte', cor: '#009974' },
];

function avaliarForcaSenha(senha: string): ForcaSenha | null {
  if (!senha) {
    return null;
  }
  let pontos = 0;
  if (senha.length >= 8) pontos++;
  if (senha.length >= 12) pontos++;
  if (/[a-z]/.test(senha) && /[A-Z]/.test(senha)) pontos++;
  if (/\d/.test(senha)) pontos++;
  if (/[^A-Za-z0-9]/.test(senha)) pontos++;

  return NIVEIS_FORCA[Math.min(pontos, NIVEIS_FORCA.length - 1)];
}

@Component({
  selector: 'app-registro',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    AuthHero,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './registro.html',
  styleUrl: './registro.scss',
})
export class Registro {
  protected readonly carregando = signal(false);
  protected readonly sucesso = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly mostrarSenha = signal(false);
  protected readonly senhaDigitada = signal('');
  protected readonly forcaSenha = computed(() => avaliarForcaSenha(this.senhaDigitada()));

  private readonly fb = inject(FormBuilder);

  protected readonly form = this.fb.nonNullable.group({
    nome: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    senha: ['', [Validators.required, Validators.minLength(8)]],
  });

  constructor(
    private readonly authService: AuthService,
    private readonly router: Router
  ) {}

  submeter(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.carregando.set(true);
    this.erro.set(null);
    const { nome, email, senha } = this.form.getRawValue();

    this.authService.registrar({ nome, email, senha }).subscribe({
      next: () => {
        this.authService.login({ email, senha }).subscribe({
          next: () => {
            this.carregando.set(false);
            this.sucesso.set(true);
            setTimeout(() => this.router.navigate(['/resumo']), 550);
          },
          error: (erro) => {
            this.carregando.set(false);
            this.erro.set(mensagemDeErro(erro));
          },
        });
      },
      error: (erro) => {
        this.carregando.set(false);
        this.erro.set(mensagemDeErro(erro));
      },
    });
  }
}
