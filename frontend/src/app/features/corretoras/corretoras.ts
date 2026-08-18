import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import { CorretoraService } from '../../core/services/corretora.service';
import { mensagemDeErro } from '../../core/services/erro.util';
import { CorretoraResponse } from '../../core/models/corretora.model';

@Component({
  selector: 'app-corretoras',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatTableModule,
  ],
  templateUrl: './corretoras.html',
  styleUrl: './corretoras.scss',
})
export class Corretoras implements OnInit {
  protected readonly corretoras = signal<CorretoraResponse[]>([]);
  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly sucesso = signal<string | null>(null);

  protected readonly colunas = ['razaoSocial', 'cnpj', 'cidade', 'uf', 'validadaCvm'];

  private readonly fb = inject(FormBuilder);

  protected readonly form = this.fb.nonNullable.group({
    cnpj: ['', Validators.required],
    cep: ['', Validators.required],
    numero: [''],
    complemento: [''],
    email: [''],
    telefone: [''],
  });

  constructor(private readonly corretoraService: CorretoraService) {}

  ngOnInit(): void {
    this.carregar();
  }

  submeter(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.enviando.set(true);
    this.erro.set(null);
    this.sucesso.set(null);
    const valores = this.form.getRawValue();

    this.corretoraService
      .cadastrar({
        cnpj: valores.cnpj,
        cep: valores.cep,
        numero: valores.numero || null,
        complemento: valores.complemento || null,
        email: valores.email || null,
        telefone: valores.telefone || null,
      })
      .subscribe({
        next: (corretora) => {
          this.enviando.set(false);
          this.sucesso.set(`Corretora "${corretora.razaoSocial}" cadastrada com sucesso.`);
          this.form.reset();
          this.carregar();
        },
        error: (erro) => {
          this.enviando.set(false);
          this.erro.set(mensagemDeErro(erro));
        },
      });
  }

  private carregar(): void {
    this.corretoraService.listar(0, 20).subscribe((pagina) => this.corretoras.set(pagina.content));
  }
}