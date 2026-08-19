import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import { CarteiraService } from '../../core/services/carteira.service';
import { mensagemDeErro } from '../../core/services/erro.util';
import { PosicaoResponse } from '../../core/models/carteira.model';

interface LinhaRebalanceamento {
  ticker: string;
  nomeEmpresa: string | null;
  valorAtual: number;
  percentualAtual: number;
  meta: number;
  valorAlvo: number;
  diferenca: number;
  sugestaoAporte: number;
}

const CHAVE_METAS_LOCALSTORAGE = 'gestao-acoes:metas-alocacao';

@Component({
  selector: 'app-rebalanceamento',
  imports: [
    CurrencyPipe,
    DecimalPipe,
    FormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatTableModule,
  ],
  templateUrl: './rebalanceamento.html',
  styleUrl: './rebalanceamento.scss',
})
export class Rebalanceamento implements OnInit {
  protected readonly carregando = signal(true);
  protected readonly erro = signal<string | null>(null);
  protected readonly posicoes = signal<PosicaoResponse[]>([]);
  protected readonly metas = signal<Record<string, number>>({});
  protected readonly aporte = signal<number>(0);

  protected readonly colunas = ['ticker', 'percentualAtual', 'valorAtual', 'meta', 'valorAlvo', 'diferenca', 'sugestaoAporte'];

  protected readonly totalAtual = computed(() => this.posicoes().reduce((soma, p) => soma + p.valorAtual, 0));

  protected readonly somaMetas = computed(() => {
    const metasAtuais = this.metas();
    return this.posicoes().reduce((soma, p) => soma + (metasAtuais[p.ticker] ?? 0), 0);
  });

  protected readonly metasEquilibradas = computed(() => Math.abs(this.somaMetas() - 100) < 0.5);

  protected readonly linhas = computed<LinhaRebalanceamento[]>(() => {
    const total = this.totalAtual();
    const baseComAporte = total + this.aporte();
    const metasAtuais = this.metas();

    const semSugestao = this.posicoes().map((p) => {
      const percentualAtual = total > 0 ? (p.valorAtual / total) * 100 : 0;
      const meta = metasAtuais[p.ticker] ?? 0;
      const valorAlvo = (baseComAporte * meta) / 100;
      return {
        ticker: p.ticker,
        nomeEmpresa: p.nomeEmpresa,
        valorAtual: p.valorAtual,
        percentualAtual,
        meta,
        valorAlvo,
        diferenca: valorAlvo - p.valorAtual,
        sugestaoAporte: 0,
      };
    });

    const sugestoes = this.distribuirAporte(
      semSugestao.map((l) => ({ ticker: l.ticker, capacidade: Math.max(0, l.diferenca) })),
      this.aporte()
    );

    return semSugestao.map((l) => ({ ...l, sugestaoAporte: sugestoes.get(l.ticker) ?? 0 }));
  });

  protected readonly totalSugerido = computed(() => this.linhas().reduce((soma, l) => soma + l.sugestaoAporte, 0));
  protected readonly aporteNaoAlocado = computed(() => Math.max(0, this.aporte() - this.totalSugerido()));

  constructor(private readonly carteiraService: CarteiraService) {}

  ngOnInit(): void {
    this.metas.set(this.carregarMetasSalvas());
    this.carteiraService.listarPosicoes().subscribe({
      next: (posicoes) => {
        this.posicoes.set(posicoes);
        this.preencherMetasFaltantes(posicoes);
        this.carregando.set(false);
      },
      error: (erro) => {
        this.erro.set(mensagemDeErro(erro));
        this.carregando.set(false);
      },
    });
  }

  protected atualizarMeta(ticker: string, valor: number): void {
    const novoValor = Number.isFinite(valor) && valor >= 0 ? valor : 0;
    this.metas.update((atuais) => ({ ...atuais, [ticker]: novoValor }));
    this.salvarMetas();
  }

  protected atualizarAporte(valor: number): void {
    this.aporte.set(Number.isFinite(valor) && valor >= 0 ? valor : 0);
  }

  // Ponto de partida comum: metas iguais à alocação de hoje (nada a comprar/vender até o usuário ajustar).
  protected distribuirComoHoje(): void {
    const total = this.totalAtual();
    const novasMetas: Record<string, number> = {};
    for (const p of this.posicoes()) {
      novasMetas[p.ticker] = total > 0 ? Math.round((p.valorAtual / total) * 10000) / 100 : 0;
    }
    this.metas.set(novasMetas);
    this.salvarMetas();
  }

  // Outro ponto de partida comum: peso igual entre todos os ativos.
  protected distribuirIgualmente(): void {
    const quantidade = this.posicoes().length;
    if (quantidade === 0) {
      return;
    }
    const percentualIgual = Math.round((100 / quantidade) * 100) / 100;
    const novasMetas: Record<string, number> = {};
    for (const p of this.posicoes()) {
      novasMetas[p.ticker] = percentualIgual;
    }
    this.metas.set(novasMetas);
    this.salvarMetas();
  }

  private preencherMetasFaltantes(posicoes: PosicaoResponse[]): void {
    const total = posicoes.reduce((soma, p) => soma + p.valorAtual, 0);
    const atuais = this.metas();
    const faltantes = posicoes.some((p) => !(p.ticker in atuais));
    if (!faltantes) {
      return;
    }
    const completas = { ...atuais };
    for (const p of posicoes) {
      if (!(p.ticker in completas)) {
        completas[p.ticker] = total > 0 ? Math.round((p.valorAtual / total) * 10000) / 100 : 0;
      }
    }
    this.metas.set(completas);
    this.salvarMetas();
  }

  private carregarMetasSalvas(): Record<string, number> {
    try {
      const bruto = localStorage.getItem(CHAVE_METAS_LOCALSTORAGE);
      return bruto ? (JSON.parse(bruto) as Record<string, number>) : {};
    } catch {
      return {};
    }
  }

  private salvarMetas(): void {
    try {
      localStorage.setItem(CHAVE_METAS_LOCALSTORAGE, JSON.stringify(this.metas()));
    } catch {
      // Armazenamento indisponível (ex.: modo privado) — a meta continua válida apenas nesta sessão.
    }
  }

  // "Water-filling": distribui o aporte em rodadas iguais entre os ativos abaixo da meta,
  // removendo da rodada seguinte quem já atingiu sua capacidade (o alvo). Isso prioriza
  // naturalmente quem está mais defasado, sem vender nada.
  private distribuirAporte(itens: { ticker: string; capacidade: number }[], aporte: number): Map<string, number> {
    const alocado = new Map<string, number>(itens.map((i) => [i.ticker, 0]));
    let restante = aporte;
    let ativos = itens.filter((i) => i.capacidade > 0.01);

    while (restante > 0.01 && ativos.length > 0) {
      const parcela = restante / ativos.length;
      let distribuido = 0;
      const proximaRodada: typeof ativos = [];

      for (const item of ativos) {
        const espacoLivre = item.capacidade - (alocado.get(item.ticker) ?? 0);
        const aplicar = Math.min(parcela, espacoLivre);
        alocado.set(item.ticker, (alocado.get(item.ticker) ?? 0) + aplicar);
        distribuido += aplicar;
        if (espacoLivre - aplicar > 0.01) {
          proximaRodada.push(item);
        }
      }

      restante -= distribuido;
      ativos = proximaRodada;
    }

    return alocado;
  }
}
