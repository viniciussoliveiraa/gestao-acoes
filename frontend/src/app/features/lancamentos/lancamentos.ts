import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatNativeDateModule } from '@angular/material/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { AcaoService } from '../../core/services/acao.service';
import { CarteiraService } from '../../core/services/carteira.service';
import { CorretoraService } from '../../core/services/corretora.service';
import { mensagemDeErro } from '../../core/services/erro.util';
import { AcaoResponse } from '../../core/models/acao.model';
import { CorretoraResponse } from '../../core/models/corretora.model';
import { LancamentoResponse } from '../../core/models/carteira.model';

@Component({
  selector: 'app-lancamentos',
  imports: [
    CurrencyPipe,
    DatePipe,
    DecimalPipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatNativeDateModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatTableModule,
  ],
  templateUrl: './lancamentos.html',
  styleUrl: './lancamentos.scss',
})
export class Lancamentos implements OnInit {
  protected readonly acoes = signal<AcaoResponse[]>([]);
  protected readonly corretoras = signal<CorretoraResponse[]>([]);
  protected readonly lancamentos = signal<LancamentoResponse[]>([]);
  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly sucesso = signal<string | null>(null);

  protected readonly colunas = ['tickerAcao', 'razaoSocialCorretora', 'quantidade', 'precoUnitario', 'dataOperacao'];

  private readonly fb = inject(FormBuilder);

  protected readonly form = this.fb.nonNullable.group({
    acaoId: [null as number | null, Validators.required],
    corretoraId: [null as number | null, Validators.required],
    quantidade: [null as number | null, [Validators.required, Validators.min(0.00000001)]],
    precoUnitario: [null as number | null, [Validators.required, Validators.min(0.01)]],
    dataOperacao: [null as Date | null, Validators.required],
  });

  constructor(
    private readonly acaoService: AcaoService,
    private readonly corretoraService: CorretoraService,
    private readonly carteiraService: CarteiraService
  ) {}

  ngOnInit(): void {
    this.acaoService.listar(0, 100).subscribe((pagina) => this.acoes.set(pagina.content));
    this.corretoraService.listar(0, 100).subscribe((pagina) => this.corretoras.set(pagina.content));
    this.carregarLancamentos();
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

    this.carteiraService
      .registrarLancamento({
        acaoId: valores.acaoId!,
        corretoraId: valores.corretoraId!,
        quantidade: valores.quantidade!,
        precoUnitario: valores.precoUnitario!,
        dataOperacao: this.formatarData(valores.dataOperacao!),
      })
      .subscribe({
        next: () => {
          this.enviando.set(false);
          this.sucesso.set('Lançamento registrado com sucesso.');
          this.form.reset();
          this.carregarLancamentos();
        },
        error: (erro) => {
          this.enviando.set(false);
          this.erro.set(mensagemDeErro(erro));
        },
      });
  }

  private carregarLancamentos(): void {
    this.carteiraService.listarLancamentos(0, 20).subscribe((pagina) => this.lancamentos.set(pagina.content));
  }

  private formatarData(data: Date): string {
    const ano = data.getFullYear();
    const mes = String(data.getMonth() + 1).padStart(2, '0');
    const dia = String(data.getDate()).padStart(2, '0');
    return `${ano}-${mes}-${dia}`;
  }
}