import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatNativeDateModule } from '@angular/material/core';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { AcaoService } from '../../core/services/acao.service';
import { CarteiraService } from '../../core/services/carteira.service';
import { CorretoraService } from '../../core/services/corretora.service';
import { mensagemDeErro } from '../../core/services/erro.util';
import { AcaoResponse } from '../../core/models/acao.model';
import { CorretoraResponse } from '../../core/models/corretora.model';
import { LancamentoResponse, TipoLancamento } from '../../core/models/carteira.model';

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
    MatPaginatorModule,
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

  // Lista completa (não paginada) só pra alimentar os totais do resumo — a
  // tabela em si usa a página atual, retornada pelo backend.
  protected readonly todosLancamentos = signal<LancamentoResponse[]>([]);
  protected readonly totalInvestido = computed(() =>
    this.todosLancamentos().reduce((total, l) => total + l.quantidade * l.precoUnitario, 0)
  );
  protected readonly quantidadeLancamentos = computed(() => this.todosLancamentos().length);
  protected readonly ticketMedio = computed(() => {
    const quantidade = this.quantidadeLancamentos();
    return quantidade > 0 ? this.totalInvestido() / quantidade : 0;
  });

  protected readonly totalElementos = signal(0);
  protected readonly pageIndex = signal(0);
  protected readonly pageSize = signal(10);

  protected readonly colunas = ['tipo', 'tickerAcao', 'razaoSocialCorretora', 'quantidade', 'precoUnitario', 'dataOperacao'];

  private readonly fb = inject(FormBuilder);

  protected readonly form = this.fb.nonNullable.group({
    acaoId: [null as number | null, Validators.required],
    corretoraId: [null as number | null, Validators.required],
    tipo: ['COMPRA' as TipoLancamento, Validators.required],
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
    this.carregarTotais();
  }

  protected onPageChange(evento: PageEvent): void {
    this.pageIndex.set(evento.pageIndex);
    this.pageSize.set(evento.pageSize);
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
        tipo: valores.tipo,
        quantidade: valores.quantidade!,
        precoUnitario: valores.precoUnitario!,
        dataOperacao: this.formatarData(valores.dataOperacao!),
      })
      .subscribe({
        next: () => {
          this.enviando.set(false);
          this.sucesso.set('Lançamento registrado com sucesso.');
          this.form.reset();
          this.pageIndex.set(0);
          this.carregarLancamentos();
          this.carregarTotais();
        },
        error: (erro) => {
          this.enviando.set(false);
          this.erro.set(mensagemDeErro(erro));
        },
      });
  }

  private carregarLancamentos(): void {
    this.carteiraService.listarLancamentos(this.pageIndex(), this.pageSize()).subscribe((pagina) => {
      this.lancamentos.set(pagina.content);
      this.totalElementos.set(pagina.totalElements);
    });
  }

  private carregarTotais(): void {
    this.carteiraService.listarLancamentos(0, 1000).subscribe((pagina) => this.todosLancamentos.set(pagina.content));
  }

  private formatarData(data: Date): string {
    const ano = data.getFullYear();
    const mes = String(data.getMonth() + 1).padStart(2, '0');
    const dia = String(data.getDate()).padStart(2, '0');
    return `${ano}-${mes}-${dia}`;
  }
}