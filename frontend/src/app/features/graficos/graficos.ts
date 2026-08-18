import { Component, OnInit, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { ChartConfiguration, ChartData } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';
import { CarteiraService } from '../../core/services/carteira.service';
import { LancamentoResponse, PosicaoResponse } from '../../core/models/carteira.model';

@Component({
  selector: 'app-graficos',
  imports: [MatIconModule, BaseChartDirective],
  templateUrl: './graficos.html',
  styleUrl: './graficos.scss',
})
export class Graficos implements OnInit {
  protected readonly semDados = signal(true);

  protected readonly alocacaoData = signal<ChartData<'doughnut'>>({ labels: [], datasets: [{ data: [] }] });
  protected readonly alocacaoOptions: ChartConfiguration<'doughnut'>['options'] = {
    responsive: true,
    plugins: { legend: { position: 'right' } },
  };

  protected readonly evolucaoData = signal<ChartData<'line'>>({ labels: [], datasets: [] });
  protected readonly evolucaoOptions: ChartConfiguration<'line'>['options'] = {
    responsive: true,
    plugins: { legend: { display: false } },
    scales: { y: { beginAtZero: true } },
  };

  constructor(private readonly carteiraService: CarteiraService) {}

  ngOnInit(): void {
    this.carteiraService.listarPosicoes().subscribe((posicoes) => this.montarAlocacao(posicoes));
    this.carteiraService.listarLancamentos(0, 200).subscribe((pagina) => this.montarEvolucao(pagina.content));
  }

  // Paleta própria (não os defaults do Chart.js) para o donut ficar consistente com o
  // restante da interface.
  private static readonly PALETA = [
    '#a38c65',
    '#009974',
    '#2f3137',
    '#d3b583',
    '#cb0b38',
    '#6b7280',
    '#00695f',
    '#8a7454',
  ];

  private montarAlocacao(posicoes: PosicaoResponse[]): void {
    if (posicoes.length > 0) {
      this.semDados.set(false);
    }
    this.alocacaoData.set({
      labels: posicoes.map((p) => p.ticker),
      datasets: [
        {
          data: posicoes.map((p) => p.valorAtual),
          backgroundColor: posicoes.map((_, i) => Graficos.PALETA[i % Graficos.PALETA.length]),
          borderWidth: 0,
        },
      ],
    });
  }

  private montarEvolucao(lancamentos: LancamentoResponse[]): void {
    const ordenados = [...lancamentos].sort(
      (a, b) => new Date(a.dataOperacao).getTime() - new Date(b.dataOperacao).getTime()
    );

    let acumulado = 0;
    const pontos = ordenados.map((l) => {
      acumulado += l.quantidade * l.precoUnitario;
      return { data: l.dataOperacao, valor: acumulado };
    });

    this.evolucaoData.set({
      labels: pontos.map((p) => p.data),
      datasets: [
        {
          label: 'Valor investido acumulado',
          data: pontos.map((p) => p.valor),
          borderColor: '#8a7454',
          backgroundColor: 'rgba(163, 140, 101, 0.15)',
          fill: true,
          tension: 0.2,
        },
      ],
    });
  }
}