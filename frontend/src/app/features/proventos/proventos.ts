import { CurrencyPipe, DatePipe } from '@angular/common';
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
import { ProventoService } from '../../core/services/provento.service';
import { mensagemDeErro } from '../../core/services/erro.util';
import { AcaoResponse } from '../../core/models/acao.model';
import { ProventoResponse, TipoProvento } from '../../core/models/provento.model';

@Component({
  selector: 'app-proventos',
  imports: [
    CurrencyPipe,
    DatePipe,
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
  templateUrl: './proventos.html',
  styleUrl: './proventos.scss',
})
export class Proventos implements OnInit {
  protected readonly acoes = signal<AcaoResponse[]>([]);
  protected readonly proventos = signal<ProventoResponse[]>([]);
  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly sucesso = signal<string | null>(null);

  protected readonly tipos: TipoProvento[] = ['DIVIDENDO', 'JCP'];
  protected readonly colunas = ['tickerAcao', 'tipo', 'valorTotal', 'dataPagamento'];

  private readonly fb = inject(FormBuilder);

  protected readonly form = this.fb.nonNullable.group({
    acaoId: [null as number | null, Validators.required],
    tipo: ['DIVIDENDO' as TipoProvento, Validators.required],
    valorTotal: [null as number | null, [Validators.required, Validators.min(0.01)]],
    dataPagamento: [null as Date | null, Validators.required],
  });

  constructor(
    private readonly acaoService: AcaoService,
    private readonly proventoService: ProventoService
  ) {}

  ngOnInit(): void {
    this.acaoService.listar(0, 100).subscribe((pagina) => this.acoes.set(pagina.content));
    this.carregarProventos();
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

    this.proventoService
      .registrar({
        acaoId: valores.acaoId!,
        tipo: valores.tipo,
        valorTotal: valores.valorTotal!,
        dataPagamento: this.formatarData(valores.dataPagamento!),
      })
      .subscribe({
        next: () => {
          this.enviando.set(false);
          this.sucesso.set('Provento registrado com sucesso.');
          this.form.reset({ tipo: 'DIVIDENDO' });
          this.carregarProventos();
        },
        error: (erro) => {
          this.enviando.set(false);
          this.erro.set(mensagemDeErro(erro));
        },
      });
  }

  private carregarProventos(): void {
    this.proventoService.listar(0, 20).subscribe((pagina) => this.proventos.set(pagina.content));
  }

  private formatarData(data: Date): string {
    const ano = data.getFullYear();
    const mes = String(data.getMonth() + 1).padStart(2, '0');
    const dia = String(data.getDate()).padStart(2, '0');
    return `${ano}-${mes}-${dia}`;
  }
}