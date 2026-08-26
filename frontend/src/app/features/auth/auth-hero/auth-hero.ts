import {
  ApplicationRef,
  Component,
  ElementRef,
  HostListener,
  Input,
  OnDestroy,
  computed,
  inject,
  signal,
} from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

interface TickerItem {
  simbolo: string;
  alta: boolean;
  variacao: number;
}

const TICKERS: TickerItem[] = [
  { simbolo: 'PETR4', alta: true, variacao: 1.8 },
  { simbolo: 'VALE3', alta: false, variacao: -0.6 },
  { simbolo: 'ITUB4', alta: true, variacao: 0.9 },
  { simbolo: 'BBAS3', alta: true, variacao: 2.3 },
  { simbolo: 'WEGE3', alta: false, variacao: -1.2 },
  { simbolo: 'AAPL', alta: true, variacao: 0.4 },
  { simbolo: 'MSFT', alta: true, variacao: 1.1 },
  { simbolo: 'TSLA', alta: false, variacao: -2.7 },
  { simbolo: 'ABEV3', alta: true, variacao: 0.3 },
  { simbolo: 'B3SA3', alta: false, variacao: -0.8 },
];

interface NotificacaoItem {
  icone: string;
  texto: string;
}

// Feed de "atividade recente" decorativo — não são eventos reais, só reforça
// visualmente as funcionalidades do produto (compra, provento, rebalanceamento).
const NOTIFICACOES: NotificacaoItem[] = [
  { icone: 'check_circle', texto: 'Compra de 10 PETR4 confirmada' },
  { icone: 'payments', texto: 'Provento de VALE3: R$ 84,20' },
  { icone: 'balance', texto: 'Rebalanceamento sugerido: RF +3%' },
  { icone: 'trending_up', texto: 'Carteira atingiu novo recorde' },
  { icone: 'payments', texto: 'Provento de ITUB4: R$ 32,50' },
];

// Número ilustrativo (não é uma cifra real da plataforma) — só pra dar peso
// visual ao selo "sob gestão".
const VALOR_SOB_GESTAO = 128450;

@Component({
  selector: 'app-auth-hero',
  imports: [MatIconModule],
  templateUrl: './auth-hero.html',
  styleUrl: './auth-hero.scss',
})
export class AuthHero implements OnDestroy {
  @Input({ required: true }) titulo!: string;
  @Input({ required: true }) subtitulo!: string;

  // Faixa decorativa (não são cotações reais) — dobrada para permitir o loop contínuo.
  protected readonly tickers = [...TICKERS, ...TICKERS];

  protected readonly parallax = signal({ x: 0, y: 0 });
  protected readonly spotlight = signal({ x: 50, y: 35 });

  private readonly notificacaoIndex = signal(0);
  protected readonly notificacaoAtual = computed(() => NOTIFICACOES[this.notificacaoIndex()]);

  private readonly elementRef = inject(ElementRef<HTMLElement>);
  private readonly prefereMenosMovimento =
    typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  // App roda em modo zoneless (sem zone.js): setInterval/requestAnimationFrame
  // "crus" atualizam os signals, mas nada aciona um novo ciclo de detecção de
  // mudanças sozinho — por isso o tick() manual depois de cada write.
  private readonly applicationRef = inject(ApplicationRef);

  // Conta de 0 até o valor final quando a tela carrega, em vez de já nascer
  // com o número pronto — dá mais peso ao dado sem exigir nenhum estado extra.
  protected readonly valorGerido = signal(this.prefereMenosMovimento ? VALOR_SOB_GESTAO : 0);
  protected readonly valorGeridoFormatado = computed(() =>
    this.valorGerido().toLocaleString('pt-BR', { maximumFractionDigits: 0 })
  );

  private iniciarContagemValorGerido(): void {
    if (this.prefereMenosMovimento || typeof window === 'undefined') {
      return;
    }
    const duracaoMs = 1400;
    const inicio = performance.now();
    const passo = (agora: number): void => {
      const progresso = Math.min((agora - inicio) / duracaoMs, 1);
      const facilitado = 1 - Math.pow(1 - progresso, 3);
      this.valorGerido.set(Math.round(VALOR_SOB_GESTAO * facilitado));
      this.applicationRef.tick();
      if (progresso < 1) {
        requestAnimationFrame(passo);
      }
    };
    requestAnimationFrame(passo);
  }

  private readonly notificacaoTimer: ReturnType<typeof setInterval> | null =
    typeof window !== 'undefined' && !this.prefereMenosMovimento
      ? setInterval(() => {
          this.notificacaoIndex.update((indice) => (indice + 1) % NOTIFICACOES.length);
          this.applicationRef.tick();
        }, 3400)
      : null;

  constructor() {
    this.iniciarContagemValorGerido();
  }

  ngOnDestroy(): void {
    if (this.notificacaoTimer !== null) {
      clearInterval(this.notificacaoTimer);
    }
  }

  @HostListener('mousemove', ['$event'])
  protected aoMoverMouse(event: MouseEvent): void {
    if (this.prefereMenosMovimento) {
      return;
    }
    const deslocamentoX = (event.clientX / window.innerWidth - 0.5) * 18;
    const deslocamentoY = (event.clientY / window.innerHeight - 0.5) * 18;
    this.parallax.set({ x: deslocamentoX, y: deslocamentoY });

    const limites = this.elementRef.nativeElement.getBoundingClientRect();
    this.spotlight.set({
      x: ((event.clientX - limites.left) / limites.width) * 100,
      y: ((event.clientY - limites.top) / limites.height) * 100,
    });
  }
}
