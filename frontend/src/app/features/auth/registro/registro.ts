import { Component, inject, signal } from '@angular/core';
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

@Component({
  selector: 'app-registro',
  imports: [
    ReactiveFormsModule,
    RouterLink,
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
  protected readonly erro = signal<string | null>(null);

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
            this.router.navigate(['/resumo']);
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