import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import { CarteiraService } from '../../core/services/carteira.service';
import { mensagemDeErro } from '../../core/services/erro.util';
import { PosicaoResponse } from '../../core/models/carteira.model';

@Component({
  selector: 'app-resumo',
  imports: [CurrencyPipe, DecimalPipe, MatIconModule, MatProgressSpinnerModule, MatTableModule],
  templateUrl: './resumo.html',
  styleUrl: './resumo.scss',
})
export class Resumo implements OnInit {
  protected readonly posicoes = signal<PosicaoResponse[]>([]);
  protected readonly carregando = signal(true);
  protected readonly erro = signal<string | null>(null);

  protected readonly colunas = [
    'ticker',
    'quantidade',
    'precoMedio',
    'valorInvestido',
    'valorAtual',
    'variacaoPercentual',
  ];

  constructor(private readonly carteiraService: CarteiraService) {}

  ngOnInit(): void {
    this.carteiraService.listarPosicoes().subscribe({
      next: (posicoes) => {
        this.posicoes.set(posicoes);
        this.carregando.set(false);
      },
      error: (erro) => {
        this.erro.set(mensagemDeErro(erro));
        this.carregando.set(false);
      },
    });
  }

  get valorTotalInvestido(): number {
    return this.posicoes().reduce((total, p) => total + p.valorInvestido, 0);
  }

  get valorTotalAtual(): number {
    return this.posicoes().reduce((total, p) => total + p.valorAtual, 0);
  }

  get variacaoTotal(): number {
    const investido = this.valorTotalInvestido;
    return investido === 0 ? 0 : ((this.valorTotalAtual - investido) * 100) / investido;
  }
}