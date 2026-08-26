import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../../core/services/auth.service';
import { mensagemDeErro } from '../../../core/services/erro.util';
import { AuthHero } from '../auth-hero/auth-hero';

@Component({
  selector: 'app-login',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    AuthHero,
    MatButtonModule,
    MatCardModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login {
  protected readonly carregando = signal(false);
  protected readonly sucesso = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly mostrarSenha = signal(false);
  private readonly saudacaoInfo = this.saudacaoPorHorario();
  protected readonly saudacao = this.saudacaoInfo.texto;
  protected readonly saudacaoIcone = this.saudacaoInfo.icone;

  private readonly fb = inject(FormBuilder);

  protected readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    senha: ['', Validators.required],
    lembrar: [true],
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
    const { email, senha, lembrar } = this.form.getRawValue();

    this.authService.login({ email, senha }, lembrar).subscribe({
      next: () => {
        this.carregando.set(false);
        this.sucesso.set(true);
        // Segura um instante pra o usuário ver a confirmação antes de sair da tela.
        setTimeout(() => this.router.navigate(['/resumo']), 550);
      },
      error: (erro) => {
        this.carregando.set(false);
        this.erro.set(mensagemDeErro(erro));
      },
    });
  }

  private saudacaoPorHorario(): { texto: string; icone: string } {
    const hora = new Date().getHours();
    if (hora < 12) {
      return { texto: 'Bom dia', icone: 'wb_twilight' };
    }
    if (hora < 18) {
      return { texto: 'Boa tarde', icone: 'wb_sunny' };
    }
    return { texto: 'Boa noite', icone: 'bedtime' };
  }
}
