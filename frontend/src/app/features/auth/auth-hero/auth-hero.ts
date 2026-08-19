import { Component, ElementRef, HostListener, Input, inject, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

interface TickerItem {
  simbolo: string;
  alta: boolean;
}

const TICKERS: TickerItem[] = [
  { simbolo: 'PETR4', alta: true },
  { simbolo: 'VALE3', alta: false },
  { simbolo: 'ITUB4', alta: true },
  { simbolo: 'BBAS3', alta: true },
  { simbolo: 'WEGE3', alta: false },
  { simbolo: 'AAPL', alta: true },
  { simbolo: 'MSFT', alta: true },
  { simbolo: 'TSLA', alta: false },
  { simbolo: 'ABEV3', alta: true },
  { simbolo: 'B3SA3', alta: false },
];

@Component({
  selector: 'app-auth-hero',
  imports: [MatIconModule],
  templateUrl: './auth-hero.html',
  styleUrl: './auth-hero.scss',
})
export class AuthHero {
  @Input({ required: true }) titulo!: string;
  @Input({ required: true }) subtitulo!: string;

  // Faixa decorativa (não são cotações reais) — dobrada para permitir o loop contínuo.
  protected readonly tickers = [...TICKERS, ...TICKERS];

  protected readonly parallax = signal({ x: 0, y: 0 });
  protected readonly spotlight = signal({ x: 50, y: 35 });

  private readonly elementRef = inject(ElementRef<HTMLElement>);
  private readonly prefereMenosMovimento =
    typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

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
