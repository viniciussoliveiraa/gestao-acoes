import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { AcaoService } from '../../core/services/acao.service';
import { mensagemDeErro } from '../../core/services/erro.util';
import { AcaoResponse, Mercado } from '../../core/models/acao.model';

@Component({
  selector: 'app-acoes',
  imports: [
    CurrencyPipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatTableModule,
  ],
  templateUrl: './acoes.html',
  styleUrl: './acoes.scss',
})
export class Acoes implements OnInit {
  protected readonly acoes = signal<AcaoResponse[]>([]);
  protected readonly enviando = signal(false);
  protected readonly atualizandoId = signal<number | null>(null);
  protected readonly erro = signal<string | null>(null);
  protected readonly sucesso = signal<string | null>(null);

  protected readonly mercados: Mercado[] = ['BRASIL', 'ESTADOS_UNIDOS'];
  protected readonly colunas = ['ticker', 'mercado', 'cotacaoAtual', 'acoes'];

  private readonly fb = inject(FormBuilder);

  protected readonly form = this.fb.nonNullable.group({
    ticker: ['', Validators.required],
    mercado: ['BRASIL' as Mercado, Validators.required],
  });

  constructor(private readonly acaoService: AcaoService) {}

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

    this.acaoService.cadastrar({ ticker: valores.ticker, mercado: valores.mercado }).subscribe({
      next: (acao) => {
        this.enviando.set(false);
        this.sucesso.set(`Ação ${acao.ticker} cadastrada com sucesso.`);
        this.form.reset({ mercado: 'BRASIL' });
        this.carregar();
      },
      error: (erro) => {
        this.enviando.set(false);
        this.erro.set(mensagemDeErro(erro));
      },
    });
  }

  atualizarCotacao(acao: AcaoResponse): void {
    this.atualizandoId.set(acao.id);
    this.acaoService.atualizarCotacao(acao.id).subscribe({
      next: () => {
        this.atualizandoId.set(null);
        this.carregar();
      },
      error: (erro) => {
        this.atualizandoId.set(null);
        this.erro.set(mensagemDeErro(erro));
      },
    });
  }

  private carregar(): void {
    this.acaoService.listar(0, 20).subscribe((pagina) => this.acoes.set(pagina.content));
  }
}