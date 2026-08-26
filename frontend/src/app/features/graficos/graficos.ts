import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, computed, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ChartConfiguration, ChartData, Plugin, ScriptableContext } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';
import { AcaoService } from '../../core/services/acao.service';
import { CarteiraService } from '../../core/services/carteira.service';
import { ProventoService } from '../../core/services/provento.service';
import { AcaoResponse } from '../../core/models/acao.model';
import { LancamentoResponse, PosicaoResponse } from '../../core/models/carteira.model';
import { ProventoResponse, TipoProvento } from '../../core/models/provento.model';

interface ItemDistribuicao {
  rotulo: string;
  valor: number;
  percentual: number;
  cor: string;
}

@Component({
  selector: 'app-graficos',
  imports: [CurrencyPipe, DecimalPipe, RouterLink, MatButtonModule, MatIconModule, BaseChartDirective],
  templateUrl: './graficos.html',
  styleUrl: './graficos.scss',
})
export class Graficos implements OnInit {
  protected readonly semDados = signal(true);
  protected readonly posicoes = signal<PosicaoResponse[]>([]);
  protected readonly lancamentos = signal<LancamentoResponse[]>([]);
  protected readonly proventos = signal<ProventoResponse[]>([]);
  protected readonly acoes = signal<AcaoResponse[]>([]);

  private static readonly MOEDA = new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  });

  private static readonly MOEDA_COMPACTA = new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
    notation: 'compact',
  });

  private static readonly FONTE = "'Figtree', 'Nunito', 'Helvetica Neue', Arial, sans-serif";

  // Paleta categórica validada (8 matizes, ordem fixa, distinguível para daltonismo) —
  // usada apenas para identidade de série nos gráficos, independente da cor de marca.
  private static readonly PALETA = [
    '#2a78d6', // azul
    '#eb6834', // laranja
    '#1baf7a', // água
    '#eda100', // amarelo
    '#e87ba4', // magenta
    '#008300', // verde
    '#4a3aa7', // violeta
    '#e34948', // vermelho
  ];
  private static readonly COR_OUTROS = '#898781';
  private static readonly LIMITE_SERIES = 7;

  // ---------- KPIs ----------

  protected readonly totalInvestido = computed(() => this.posicoes().reduce((soma, p) => soma + p.valorInvestido, 0));
  protected readonly totalAtual = computed(() => this.posicoes().reduce((soma, p) => soma + p.valorAtual, 0));
  protected readonly resultado = computed(() => this.totalAtual() - this.totalInvestido());
  protected readonly variacaoPercentual = computed(() => {
    const investido = this.totalInvestido();
    return investido > 0 ? (this.resultado() / investido) * 100 : 0;
  });
  protected readonly totalProventos = computed(() => this.proventos().reduce((soma, p) => soma + p.valorTotal, 0));
  protected readonly yieldSobreCusto = computed(() => {
    const investido = this.totalInvestido();
    return investido > 0 ? (this.totalProventos() / investido) * 100 : 0;
  });

  protected readonly melhorAtivo = computed(() =>
    this.posicoes().reduce<PosicaoResponse | null>(
      (melhor, p) => (!melhor || p.variacaoPercentual > melhor.variacaoPercentual ? p : melhor),
      null
    )
  );

  protected readonly piorAtivo = computed(() =>
    this.posicoes().reduce<PosicaoResponse | null>(
      (pior, p) => (!pior || p.variacaoPercentual < pior.variacaoPercentual ? p : pior),
      null
    )
  );

  // ---------- Alocação por ativo (donut) ----------

  protected readonly alocacaoLista = computed<ItemDistribuicao[]>(() =>
    this.distribuir(this.posicoes(), (p) => p.ticker, (p) => p.valorAtual)
  );

  protected readonly alocacaoData = computed<ChartData<'doughnut'>>(() => this.paraDoughnut(this.alocacaoLista()));

  protected readonly alocacaoOptions: ChartConfiguration<'doughnut'>['options'] = this.opcoesDoughnut();

  // Desenha o patrimônio total no centro do donut — o "buraco" deixa de ser espaço
  // vazio e passa a reforçar a leitura da carteira como um todo.
  protected readonly centroTotalPlugin: Plugin<'doughnut'> = {
    id: 'centroTotal',
    afterDraw: (chart) => {
      const { ctx, chartArea } = chart;
      if (!chartArea) {
        return;
      }
      const dados = (chart.data.datasets[0]?.data as number[]) ?? [];
      const total = dados.reduce((soma, v) => soma + (Number(v) || 0), 0);
      const cx = (chartArea.left + chartArea.right) / 2;
      const cy = (chartArea.top + chartArea.bottom) / 2;

      ctx.save();
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      ctx.fillStyle = '#898781';
      ctx.font = `600 12px ${Graficos.FONTE}`;
      ctx.fillText('Patrimônio', cx, cy - 13);
      ctx.fillStyle = '#2f3137';
      ctx.font = `700 21px ${Graficos.FONTE}`;
      ctx.fillText(Graficos.MOEDA_COMPACTA.format(total), cx, cy + 12);
      ctx.restore();
    },
  };

  // ---------- Distribuição por corretora ----------

  protected readonly corretoraLista = computed<ItemDistribuicao[]>(() =>
    this.distribuir(this.lancamentos(), (l) => l.razaoSocialCorretora, (l) => l.quantidade * l.precoUnitario)
  );

  // ---------- Diversificação por mercado (BR vs. EUA) ----------

  private static readonly ROTULO_MERCADO: Record<AcaoResponse['mercado'], string> = {
    BRASIL: 'Brasil',
    ESTADOS_UNIDOS: 'Estados Unidos',
  };
  // Cores fixas (não seguem a paleta de "Outros"): mercado é sempre binário aqui,
  // então usa os dois primeiros matizes da paleta categórica validada.
  private static readonly COR_MERCADO: Record<string, string> = {
    Brasil: Graficos.PALETA[1],
    'Estados Unidos': Graficos.PALETA[0],
  };

  protected readonly mercadoLista = computed<ItemDistribuicao[]>(() => {
    const mercadoPorAcao = new Map<number, string>();
    for (const acao of this.acoes()) {
      mercadoPorAcao.set(acao.id, Graficos.ROTULO_MERCADO[acao.mercado]);
    }

    const totais = new Map<string, number>();
    for (const p of this.posicoes()) {
      const rotulo = mercadoPorAcao.get(p.acaoId);
      if (!rotulo) {
        continue;
      }
      totais.set(rotulo, (totais.get(rotulo) ?? 0) + p.valorAtual);
    }

    const total = [...totais.values()].reduce((soma, v) => soma + v, 0);
    if (total <= 0) {
      return [];
    }

    return [...totais.entries()]
      .sort((a, b) => b[1] - a[1])
      .map(([rotulo, valor]) => ({
        rotulo,
        valor,
        percentual: (valor / total) * 100,
        cor: Graficos.COR_MERCADO[rotulo] ?? Graficos.COR_OUTROS,
      }));
  });

  // ---------- Evolução do valor investido (linha) ----------

  protected readonly evolucaoData = computed<ChartData<'line'>>(() => {
    const ordenados = [...this.lancamentos()].sort(
      (a, b) => new Date(a.dataOperacao).getTime() - new Date(b.dataOperacao).getTime()
    );

    let acumulado = 0;
    const pontos = ordenados.map((l) => {
      acumulado += l.quantidade * l.precoUnitario;
      return { data: l.dataOperacao, valor: acumulado };
    });

    return {
      labels: pontos.map((p) => new Date(p.data).toLocaleDateString('pt-BR', { day: '2-digit', month: 'short' })),
      datasets: [
        {
          label: 'Valor investido acumulado',
          data: pontos.map((p) => p.valor),
          borderColor: '#8a7454',
          backgroundColor: (context: ScriptableContext<'line'>) => {
            const { chart } = context;
            const { ctx, chartArea } = chart;
            if (!chartArea) {
              return 'rgba(138, 116, 84, 0.2)';
            }
            const gradiente = ctx.createLinearGradient(0, chartArea.top, 0, chartArea.bottom);
            gradiente.addColorStop(0, 'rgba(138, 116, 84, 0.32)');
            gradiente.addColorStop(1, 'rgba(138, 116, 84, 0)');
            return gradiente;
          },
          pointBackgroundColor: '#8a7454',
          pointBorderColor: '#ffffff',
          pointBorderWidth: 2,
          pointRadius: pontos.length > 1 ? 3 : 4,
          pointHoverRadius: 6,
          borderWidth: 2.5,
          fill: true,
          tension: 0.35,
        },
      ],
    };
  });

  protected readonly evolucaoOptions: ChartConfiguration<'line'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    interaction: { mode: 'index', intersect: false },
    plugins: {
      legend: { display: false },
      tooltip: {
        backgroundColor: '#2f3137',
        titleFont: { family: Graficos.FONTE, weight: 'bold' },
        bodyFont: { family: Graficos.FONTE },
        padding: 12,
        cornerRadius: 8,
        callbacks: {
          label: (ctx) => ` ${Graficos.MOEDA.format(Number(ctx.raw) || 0)}`,
        },
      },
    },
    scales: {
      x: {
        grid: { display: false },
        ticks: { font: { family: Graficos.FONTE, size: 12 }, color: '#6b6f76' },
      },
      y: {
        beginAtZero: true,
        grid: { color: '#e3e2df' },
        border: { display: false },
        ticks: {
          font: { family: Graficos.FONTE, size: 12 },
          color: '#6b6f76',
          callback: (valor) => Graficos.MOEDA_COMPACTA.format(Number(valor)),
        },
      },
    },
  };

  // ---------- Retorno por ativo (barra horizontal, cor = polaridade) ----------

  protected readonly retornoAltura = computed(() => Math.max(220, this.posicoes().length * 38));

  protected readonly retornoData = computed<ChartData<'bar'>>(() => {
    const ordenadas = [...this.posicoes()].sort((a, b) => b.variacaoPercentual - a.variacaoPercentual);
    return {
      labels: ordenadas.map((p) => p.ticker),
      datasets: [
        {
          data: ordenadas.map((p) => p.variacaoPercentual),
          backgroundColor: ordenadas.map((p) => (p.variacaoPercentual >= 0 ? '#009974' : '#cb0b38')),
          borderRadius: 4,
          barThickness: 18,
        },
      ],
    };
  });

  protected readonly retornoOptions: ChartConfiguration<'bar'>['options'] = {
    indexAxis: 'y' as const,
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false },
      tooltip: {
        backgroundColor: '#2f3137',
        titleFont: { family: Graficos.FONTE, weight: 'bold' },
        bodyFont: { family: Graficos.FONTE },
        padding: 12,
        cornerRadius: 8,
        callbacks: {
          label: (ctx) => ` ${(Number(ctx.raw) || 0).toFixed(2)}%`,
        },
      },
    },
    scales: {
      x: {
        grid: { color: '#e3e2df' },
        border: { display: false },
        ticks: {
          font: { family: Graficos.FONTE, size: 12 },
          color: '#6b6f76',
          callback: (valor) => `${valor}%`,
        },
      },
      y: {
        grid: { display: false },
        ticks: { font: { family: Graficos.FONTE, size: 12, weight: 600 }, color: '#2f3137' },
      },
    },
  };

  // ---------- Proventos recebidos por mês (barra vertical) ----------

  protected readonly proventosMensaisData = computed<ChartData<'bar'>>(() => {
    const ordenados = [...this.proventos()].sort(
      (a, b) => new Date(a.dataPagamento).getTime() - new Date(b.dataPagamento).getTime()
    );

    const totaisPorMes = new Map<string, number>();
    for (const p of ordenados) {
      const chave = p.dataPagamento.slice(0, 7);
      totaisPorMes.set(chave, (totaisPorMes.get(chave) ?? 0) + p.valorTotal);
    }

    const chaves = [...totaisPorMes.keys()];
    return {
      labels: chaves.map((chave) =>
        new Date(`${chave}-01T00:00:00`).toLocaleDateString('pt-BR', { month: 'short', year: '2-digit' })
      ),
      datasets: [
        {
          label: 'Proventos recebidos',
          data: chaves.map((chave) => totaisPorMes.get(chave) ?? 0),
          backgroundColor: '#a38c65',
          borderRadius: 4,
          maxBarThickness: 34,
        },
      ],
    };
  });

  protected readonly proventosMensaisOptions: ChartConfiguration<'bar'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false },
      tooltip: {
        backgroundColor: '#2f3137',
        titleFont: { family: Graficos.FONTE, weight: 'bold' },
        bodyFont: { family: Graficos.FONTE },
        padding: 12,
        cornerRadius: 8,
        callbacks: {
          label: (ctx) => ` ${Graficos.MOEDA.format(Number(ctx.raw) || 0)}`,
        },
      },
    },
    scales: {
      x: {
        grid: { display: false },
        ticks: { font: { family: Graficos.FONTE, size: 12 }, color: '#6b6f76' },
      },
      y: {
        beginAtZero: true,
        grid: { color: '#e3e2df' },
        border: { display: false },
        ticks: {
          font: { family: Graficos.FONTE, size: 12 },
          color: '#6b6f76',
          callback: (valor) => Graficos.MOEDA_COMPACTA.format(Number(valor)),
        },
      },
    },
  };

  // ---------- Proventos por ativo (barra horizontal) ----------

  protected readonly proventosPorAtivoAltura = computed(() => {
    const quantidadeAtivos = new Set(this.proventos().map((p) => p.tickerAcao)).size;
    return Math.max(160, quantidadeAtivos * 38);
  });

  protected readonly proventosPorAtivoData = computed<ChartData<'bar'>>(() => {
    const totais = new Map<string, number>();
    for (const p of this.proventos()) {
      totais.set(p.tickerAcao, (totais.get(p.tickerAcao) ?? 0) + p.valorTotal);
    }
    const ordenados = [...totais.entries()].sort((a, b) => b[1] - a[1]);
    return {
      labels: ordenados.map(([ticker]) => ticker),
      datasets: [
        {
          data: ordenados.map(([, valor]) => valor),
          backgroundColor: '#a38c65',
          borderRadius: 4,
          barThickness: 18,
        },
      ],
    };
  });

  protected readonly proventosPorAtivoOptions: ChartConfiguration<'bar'>['options'] = {
    indexAxis: 'y' as const,
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false },
      tooltip: {
        backgroundColor: '#2f3137',
        titleFont: { family: Graficos.FONTE, weight: 'bold' },
        bodyFont: { family: Graficos.FONTE },
        padding: 12,
        cornerRadius: 8,
        callbacks: {
          label: (ctx) => ` ${Graficos.MOEDA.format(Number(ctx.raw) || 0)}`,
        },
      },
    },
    scales: {
      x: {
        grid: { color: '#e3e2df' },
        border: { display: false },
        ticks: {
          font: { family: Graficos.FONTE, size: 12 },
          color: '#6b6f76',
          callback: (valor) => Graficos.MOEDA_COMPACTA.format(Number(valor)),
        },
      },
      y: {
        grid: { display: false },
        ticks: { font: { family: Graficos.FONTE, size: 12, weight: 600 }, color: '#2f3137' },
      },
    },
  };

  // ---------- Proventos por tipo (Dividendo vs. JCP) ----------

  private static readonly ROTULO_TIPO_PROVENTO: Record<TipoProvento, string> = {
    DIVIDENDO: 'Dividendos',
    JCP: 'JCP',
  };
  private static readonly COR_TIPO_PROVENTO: Record<string, string> = {
    Dividendos: Graficos.PALETA[2],
    JCP: Graficos.PALETA[3],
  };

  protected readonly proventosPorTipoLista = computed<ItemDistribuicao[]>(() => {
    const totais = new Map<string, number>();
    for (const p of this.proventos()) {
      const rotulo = Graficos.ROTULO_TIPO_PROVENTO[p.tipo];
      totais.set(rotulo, (totais.get(rotulo) ?? 0) + p.valorTotal);
    }

    const total = [...totais.values()].reduce((soma, v) => soma + v, 0);
    if (total <= 0) {
      return [];
    }

    return [...totais.entries()]
      .sort((a, b) => b[1] - a[1])
      .map(([rotulo, valor]) => ({
        rotulo,
        valor,
        percentual: (valor / total) * 100,
        cor: Graficos.COR_TIPO_PROVENTO[rotulo] ?? Graficos.COR_OUTROS,
      }));
  });

  constructor(
    private readonly carteiraService: CarteiraService,
    private readonly proventoService: ProventoService,
    private readonly acaoService: AcaoService
  ) {}

  ngOnInit(): void {
    this.carteiraService.listarPosicoes().subscribe((posicoes) => {
      this.posicoes.set(posicoes);
      if (posicoes.length > 0) {
        this.semDados.set(false);
      }
    });
    this.carteiraService.listarLancamentos(0, 1000).subscribe((pagina) => this.lancamentos.set(pagina.content));
    this.proventoService.listar(0, 1000).subscribe((pagina) => this.proventos.set(pagina.content));
    this.acaoService.listar(0, 1000).subscribe((pagina) => this.acoes.set(pagina.content));
  }

  // Agrupa qualquer coleção em até 7 fatias nomeadas + "Outros", com a paleta
  // categórica fixa — nunca ciclada além dos 8 matizes validados.
  private distribuir<T>(itens: readonly T[], rotuloDe: (item: T) => string, valorDe: (item: T) => number): ItemDistribuicao[] {
    const totais = new Map<string, number>();
    for (const item of itens) {
      const rotulo = rotuloDe(item);
      totais.set(rotulo, (totais.get(rotulo) ?? 0) + valorDe(item));
    }

    const total = [...totais.values()].reduce((soma, v) => soma + v, 0);
    if (total <= 0) {
      return [];
    }

    const ordenados = [...totais.entries()].sort((a, b) => b[1] - a[1]);
    const principais = ordenados.slice(0, Graficos.LIMITE_SERIES);
    const restante = ordenados.slice(Graficos.LIMITE_SERIES);

    const resultado: ItemDistribuicao[] = principais.map(([rotulo, valor], i) => ({
      rotulo,
      valor,
      percentual: (valor / total) * 100,
      cor: Graficos.PALETA[i],
    }));

    if (restante.length > 0) {
      const valorRestante = restante.reduce((soma, [, v]) => soma + v, 0);
      resultado.push({
        rotulo: `Outras (${restante.length})`,
        valor: valorRestante,
        percentual: (valorRestante / total) * 100,
        cor: Graficos.COR_OUTROS,
      });
    }

    return resultado;
  }

  private paraDoughnut(itens: ItemDistribuicao[]): ChartData<'doughnut'> {
    return {
      labels: itens.map((i) => i.rotulo),
      datasets: [
        {
          data: itens.map((i) => i.valor),
          backgroundColor: itens.map((i) => i.cor),
          borderColor: '#ffffff',
          borderWidth: 2,
          hoverOffset: 10,
          hoverBorderColor: '#ffffff',
        },
      ],
    };
  }

  private opcoesDoughnut(): ChartConfiguration<'doughnut'>['options'] {
    return {
      responsive: true,
      maintainAspectRatio: false,
      cutout: '72%',
      animation: { animateRotate: true, animateScale: true },
      plugins: {
        legend: { display: false },
        tooltip: {
          backgroundColor: '#2f3137',
          titleFont: { family: Graficos.FONTE, weight: 'bold' },
          bodyFont: { family: Graficos.FONTE },
          padding: 12,
          cornerRadius: 8,
          displayColors: true,
          boxPadding: 4,
          callbacks: {
            label: (ctx) => {
              const valores = (ctx.dataset.data as number[]).map((v) => Number(v) || 0);
              const total = valores.reduce((soma, v) => soma + v, 0);
              const valor = Number(ctx.raw) || 0;
              const percentual = total > 0 ? (valor / total) * 100 : 0;
              return ` ${ctx.label}: ${Graficos.MOEDA.format(valor)} (${percentual.toFixed(1)}%)`;
            },
          },
        },
      },
    };
  }
}
