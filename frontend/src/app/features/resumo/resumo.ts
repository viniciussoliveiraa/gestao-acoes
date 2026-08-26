import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { ApplicationRef, Component, OnInit, WritableSignal, computed, effect, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import { MatSortModule, Sort } from '@angular/material/sort';
import { CarteiraService } from '../../core/services/carteira.service';
import { ProventoService } from '../../core/services/provento.service';
import { mensagemDeErro } from '../../core/services/erro.util';
import { PosicaoResponse } from '../../core/models/carteira.model';
import { ProventoResponse } from '../../core/models/provento.model';

interface PosicaoComPeso extends PosicaoResponse {
  percentualCarteira: number;
}

type ColunaOrdenavel = keyof Pick<
  PosicaoComPeso,
  'ticker' | 'quantidade' | 'precoMedio' | 'valorInvestido' | 'valorAtual' | 'percentualCarteira' | 'variacaoPercentual'
>;

@Component({
  selector: 'app-resumo',
  imports: [
    CurrencyPipe,
    DecimalPipe,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTableModule,
    MatSortModule,
  ],
  templateUrl: './resumo.html',
  styleUrl: './resumo.scss',
})
export class Resumo implements OnInit {
  protected readonly posicoes = signal<PosicaoResponse[]>([]);
  protected readonly proventos = signal<ProventoResponse[]>([]);
  protected readonly carregando = signal(true);
  protected readonly erro = signal<string | null>(null);

  // Sobem animados de 0 até o valor real assim que os dados chegam, em vez
  // de aparecer prontos — o mesmo tratamento usado no número da tela de login.
  protected readonly valorInvestidoAnimado = signal(0);
  protected readonly valorAtualAnimado = signal(0);
  protected readonly valorProventosAnimado = signal(0);

  protected readonly colunas = [
    'ticker',
    'quantidade',
    'precoMedio',
    'valorInvestido',
    'valorAtual',
    'percentualCarteira',
    'variacaoPercentual',
  ];

  protected readonly filtro = signal('');
  protected readonly ordenacao = signal<Sort>({ active: 'valorAtual', direction: 'desc' });

  // Peso de cada posição no patrimônio total — mesma leitura da "Posição na
  // Carteira" de referência, calculada no cliente a partir das posições já carregadas.
  private readonly posicoesComPeso = computed<PosicaoComPeso[]>(() => {
    const total = this.valorTotalAtual;
    return this.posicoes().map((p) => ({
      ...p,
      percentualCarteira: total > 0 ? (p.valorAtual / total) * 100 : 0,
    }));
  });

  // Busca por ticker/empresa e ordenação por coluna — igual à tabela "Posição
  // na Carteira" de referência, tudo calculado no cliente sobre os dados já carregados.
  protected readonly linhasTabela = computed<PosicaoComPeso[]>(() =>
    this.ordenar(this.filtrar(this.posicoesComPeso(), this.filtro()), this.ordenacao())
  );

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

  protected readonly valorTotalProventos = computed(() => this.proventos().reduce((total, p) => total + p.valorTotal, 0));

  // Aviso de concentração: carteiras pouco diversificadas (um ativo dominando
  // o patrimônio) são um risco que vale sinalizar, não só reportar.
  private static readonly LIMITE_CONCENTRACAO = 40;

  protected readonly alertaConcentracao = computed(() => {
    const posicoes = this.posicoesComPeso();
    if (posicoes.length < 2) {
      return null;
    }
    const maiorPosicao = posicoes.reduce((maior, p) => (p.percentualCarteira > maior.percentualCarteira ? p : maior));
    return maiorPosicao.percentualCarteira >= Resumo.LIMITE_CONCENTRACAO ? maiorPosicao : null;
  });

  private readonly prefereMenosMovimento =
    typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  // App roda em modo zoneless (sem zone.js): um requestAnimationFrame "cru"
  // atualiza o signal, mas nada aciona um novo ciclo de detecção de mudanças
  // sozinho — por isso o tick() manual a cada quadro da animação.
  private readonly applicationRef = inject(ApplicationRef);

  constructor(
    private readonly carteiraService: CarteiraService,
    private readonly proventoService: ProventoService
  ) {
    effect(() => {
      if (this.posicoes().length === 0) {
        return;
      }
      this.animarContagem(this.valorInvestidoAnimado, this.valorTotalInvestido);
      this.animarContagem(this.valorAtualAnimado, this.valorTotalAtual);
    });
    effect(() => {
      if (this.proventos().length === 0) {
        return;
      }
      this.animarContagem(this.valorProventosAnimado, this.valorTotalProventos());
    });
  }

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
    this.proventoService.listar(0, 1000).subscribe((pagina) => this.proventos.set(pagina.content));
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

  protected onFiltroChange(event: Event): void {
    this.filtro.set((event.target as HTMLInputElement).value);
  }

  protected onOrdenacaoChange(sort: Sort): void {
    this.ordenacao.set(sort);
  }

  // Ponto-e-vírgula como separador de coluna: é o que o Excel em pt-BR espera,
  // já que a vírgula ali é o separador decimal.
  protected exportarCsv(): void {
    const cabecalho = [
      'Ativo',
      'Empresa',
      'Quantidade',
      'Preço médio',
      'Valor investido',
      'Valor atual',
      '% carteira',
      'Variação %',
    ];
    const linhas = this.linhasTabela().map((p) => [
      p.ticker,
      p.nomeEmpresa ?? '',
      p.quantidade.toString(),
      p.precoMedio.toFixed(2).replace('.', ','),
      p.valorInvestido.toFixed(2).replace('.', ','),
      p.valorAtual.toFixed(2).replace('.', ','),
      `${p.percentualCarteira.toFixed(2).replace('.', ',')}%`,
      `${p.variacaoPercentual.toFixed(2).replace('.', ',')}%`,
    ]);

    const csv = [cabecalho, ...linhas].map((linha) => linha.map((valor) => `"${valor}"`).join(';')).join('\r\n');
    const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `carteira-${new Date().toISOString().slice(0, 10)}.csv`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
  }

  private filtrar(linhas: PosicaoComPeso[], termo: string): PosicaoComPeso[] {
    const termoNormalizado = termo.trim().toLowerCase();
    if (!termoNormalizado) {
      return linhas;
    }
    return linhas.filter(
      (l) => l.ticker.toLowerCase().includes(termoNormalizado) || (l.nomeEmpresa ?? '').toLowerCase().includes(termoNormalizado)
    );
  }

  private ordenar(linhas: PosicaoComPeso[], sort: Sort): PosicaoComPeso[] {
    if (!sort.active || !sort.direction) {
      return linhas;
    }
    const coluna = sort.active as ColunaOrdenavel;
    const direcao = sort.direction === 'asc' ? 1 : -1;
    return [...linhas].sort((a, b) => {
      const valorA = a[coluna];
      const valorB = b[coluna];
      if (typeof valorA === 'string' && typeof valorB === 'string') {
        return valorA.localeCompare(valorB) * direcao;
      }
      return ((valorA as number) - (valorB as number)) * direcao;
    });
  }

  private animarContagem(alvo: WritableSignal<number>, valorFinal: number): void {
    if (this.prefereMenosMovimento || typeof window === 'undefined') {
      alvo.set(valorFinal);
      return;
    }
    const duracaoMs = 900;
    const inicio = performance.now();
    const passo = (agora: number): void => {
      const progresso = Math.min((agora - inicio) / duracaoMs, 1);
      const facilitado = 1 - Math.pow(1 - progresso, 3);
      alvo.set(valorFinal * facilitado);
      this.applicationRef.tick();
      if (progresso < 1) {
        requestAnimationFrame(passo);
      }
    };
    requestAnimationFrame(passo);
  }
}
