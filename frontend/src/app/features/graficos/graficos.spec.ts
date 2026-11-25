import { Directive, Input } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { BaseChartDirective } from 'ng2-charts';
import { of } from 'rxjs';
import { AcaoService } from '../../core/services/acao.service';
import { CarteiraService } from '../../core/services/carteira.service';
import { ProventoService } from '../../core/services/provento.service';
import { TemaService } from '../../core/services/tema.service';
import { acao, lancamento, pagina, posicao, provento } from '../../core/testing/dados';
import { Graficos } from './graficos';

/* eslint-disable @typescript-eslint/no-explicit-any */

/**
 * O jsdom não tem <canvas> de verdade, então o gráfico do ng2-charts é trocado por este falso:
 * a tela continua montando dados e opções, e o teste confere esses valores.
 */
@Directive({ selector: 'canvas[baseChart]' })
class GraficoFalso {
  @Input() data: unknown;
  @Input() options: unknown;
  @Input() type: unknown;
  @Input() plugins: unknown;
}

const petr = posicao({ acaoId: 1, ticker: 'PETR4', valorInvestido: 300, valorAtual: 600, variacaoPercentual: 100 });
const aapl = posicao({ acaoId: 2, ticker: 'AAPL', valorInvestido: 400, valorAtual: 300, variacaoPercentual: -25 });

describe('Tela Gráficos', () => {
  function criar(dados: {
    posicoes?: ReturnType<typeof posicao>[];
    lancamentos?: ReturnType<typeof lancamento>[];
    proventos?: ReturnType<typeof provento>[];
    acoes?: ReturnType<typeof acao>[];
  } = {}) {
    const {
      posicoes = [petr, aapl],
      lancamentos = [
        lancamento({ id: 2, dataOperacao: '2026-11-10', quantidade: 10, precoUnitario: 30, razaoSocialCorretora: 'XP' }),
        lancamento({ id: 1, dataOperacao: '2026-10-01', quantidade: 5, precoUnitario: 80, razaoSocialCorretora: 'BTG' }),
      ],
      proventos = [
        provento({ id: 1, tipo: 'DIVIDENDO', valorTotal: 10, dataPagamento: '2026-10-15', tickerAcao: 'PETR4' }),
        provento({ id: 2, tipo: 'JCP', valorTotal: 5, dataPagamento: '2026-11-15', tickerAcao: 'AAPL' }),
        provento({ id: 3, tipo: 'DIVIDENDO', valorTotal: 3, dataPagamento: '2026-11-20', tickerAcao: 'PETR4' }),
      ],
      acoes = [acao({ id: 1, ticker: 'PETR4' }), acao({ id: 2, ticker: 'AAPL', mercado: 'ESTADOS_UNIDOS' })],
    } = dados;

    localStorage.clear();
    TestBed.configureTestingModule({
      imports: [Graficos],
      providers: [
        provideRouter([]),
        {
          provide: CarteiraService,
          useValue: { listarPosicoes: () => of(posicoes), listarLancamentos: () => of(pagina(lancamentos)) },
        },
        { provide: ProventoService, useValue: { listar: () => of(pagina(proventos)) } },
        { provide: AcaoService, useValue: { listar: () => of(pagina(acoes)) } },
      ],
    });
    TestBed.overrideComponent(Graficos, {
      remove: { imports: [BaseChartDirective] },
      add: { imports: [GraficoFalso] },
    });
    const fixture = TestBed.createComponent(Graficos);
    fixture.detectChanges();
    return { fixture, tela: fixture.componentInstance as any, texto: () => fixture.nativeElement.textContent as string };
  }

  it('sem posições mostra o convite para cadastrar lançamentos', () => {
    const { tela, texto } = criar({ posicoes: [], lancamentos: [], proventos: [], acoes: [] });

    expect(tela.semDados()).toBe(true);
    expect(texto()).toContain('Cadastre lançamentos');
    expect(tela.variacaoPercentual()).toBe(0);
    expect(tela.yieldSobreCusto()).toBe(0);
    expect(tela.alocacaoLista()).toEqual([]);
    expect(tela.mercadoLista()).toEqual([]);
    expect(tela.proventosPorTipoLista()).toEqual([]);
  });

  it('indicadores: resultado, variação, proventos, yield, melhor e pior ativo', () => {
    const { tela, texto } = criar();

    expect(tela.semDados()).toBe(false);
    expect(tela.totalInvestido()).toBe(700);
    expect(tela.totalAtual()).toBe(900);
    expect(tela.resultado()).toBe(200);
    expect(tela.variacaoPercentual()).toBeCloseTo(28.57, 1);
    expect(tela.totalProventos()).toBe(18);
    expect(tela.yieldSobreCusto()).toBeCloseTo(2.57, 1);
    expect(tela.melhorAtivo().ticker).toBe('PETR4');
    expect(tela.piorAtivo().ticker).toBe('AAPL');
    expect(texto()).toContain('PETR4');
  });

  it('alocação: fatias com percentual e "Outras" a partir do 8º ativo', () => {
    const muitos = Array.from({ length: 9 }, (_, i) =>
      posicao({ acaoId: i + 1, ticker: `T${i + 1}`, valorAtual: 100 - i })
    );
    const { tela } = criar({ posicoes: muitos });

    const lista = tela.alocacaoLista();
    expect(lista.length).toBe(8);
    expect(lista[7].rotulo).toBe('Outras (2)');
    expect(lista[7].cor).toBe('#898781');
    expect(tela.alocacaoData().datasets[0].data.length).toBe(8);
  });

  it('mercado (Brasil x EUA) ignora ativo sem cadastro de ação', () => {
    const { tela } = criar({
      posicoes: [petr, aapl, posicao({ acaoId: 99, ticker: 'SUMIU', valorAtual: 50 })],
    });

    const mercados = tela.mercadoLista();
    expect(mercados.map((m: any) => m.rotulo)).toEqual(['Brasil', 'Estados Unidos']);
    expect(mercados[0].percentual).toBeCloseTo(66.67, 1);
  });

  it('distribuição por corretora soma quantidade x preço', () => {
    const { tela } = criar();

    expect(tela.corretoraLista().map((c: any) => [c.rotulo, c.valor])).toEqual([
      ['BTG', 400],
      ['XP', 300],
    ]);
  });

  it('evolução acumula o valor investido em ordem de data', () => {
    const { tela } = criar();

    expect(tela.evolucaoData().datasets[0].data).toEqual([400, 700]);
    // Com um ponto só, o marcador fica maior.
    TestBed.resetTestingModule();
    expect(criar({ lancamentos: [lancamento()] }).tela.evolucaoData().datasets[0].pointRadius).toBe(4);
  });

  it('retorno por ativo: verde para alta, vermelho para queda', () => {
    const { tela } = criar();

    const dados = tela.retornoData().datasets[0];
    expect(dados.data).toEqual([100, -25]);
    expect(dados.backgroundColor).toEqual(['#009974', '#cb0b38']);
    expect(tela.retornoAltura()).toBe(220);
  });

  it('proventos por mês, por ativo e por tipo', () => {
    const { tela } = criar();

    expect(tela.proventosMensaisData().datasets[0].data).toEqual([10, 8]);
    expect(tela.proventosPorAtivoData().labels).toEqual(['PETR4', 'AAPL']);
    expect(tela.proventosPorAtivoAltura()).toBe(160);
    expect(tela.proventosPorTipoLista().map((p: any) => p.rotulo)).toEqual(['Dividendos', 'JCP']);
  });

  it('textos dos tooltips e dos eixos', () => {
    const { tela } = criar();
    const ctx = { raw: 1234.5, label: 'PETR4', dataset: { data: [1234.5, 765.5] } };

    expect(tela.evolucaoOptions().plugins.tooltip.callbacks.label(ctx)).toContain('1.234,50');
    expect(tela.evolucaoOptions().scales.y.ticks.callback(2000)).toContain('2');
    expect(tela.retornoOptions().plugins.tooltip.callbacks.label({ raw: 12.345 })).toBe(' 12.35%');
    expect(tela.retornoOptions().scales.x.ticks.callback(10)).toBe('10%');
    expect(tela.proventosMensaisOptions().plugins.tooltip.callbacks.label(ctx)).toContain('1.234,50');
    expect(tela.proventosMensaisOptions().scales.y.ticks.callback(1000)).toContain('1');
    expect(tela.proventosPorAtivoOptions().plugins.tooltip.callbacks.label(ctx)).toContain('1.234,50');
    expect(tela.proventosPorAtivoOptions().scales.x.ticks.callback(1000)).toContain('1');
    expect(tela.alocacaoOptions().plugins.tooltip.callbacks.label(ctx)).toContain('(61.7%)');

    // Valores vazios/inválidos não quebram os textos.
    expect(tela.evolucaoOptions().plugins.tooltip.callbacks.label({ raw: undefined })).toContain('0,00');
    expect(tela.retornoOptions().plugins.tooltip.callbacks.label({ raw: 'x' })).toBe(' 0.00%');
    expect(tela.proventosMensaisOptions().plugins.tooltip.callbacks.label({ raw: null })).toContain('0,00');
    expect(tela.proventosPorAtivoOptions().plugins.tooltip.callbacks.label({ raw: null })).toContain('0,00');
    expect(
      tela.alocacaoOptions().plugins.tooltip.callbacks.label({ raw: null, label: 'X', dataset: { data: [null, 'a'] } })
    ).toContain('(0.0%)');
  });

  it('degradê da linha de evolução (com e sem área desenhada)', () => {
    const { tela } = criar();
    const fundo = tela.evolucaoData().datasets[0].backgroundColor;
    const gradiente = { addColorStop: vi.fn() };

    expect(fundo({ chart: { ctx: {}, chartArea: undefined } })).toBe('rgba(138, 116, 84, 0.2)');
    expect(
      fundo({ chart: { ctx: { createLinearGradient: () => gradiente }, chartArea: { top: 0, bottom: 100 } } })
    ).toBe(gradiente);
    expect(gradiente.addColorStop).toHaveBeenCalledTimes(2);
  });

  it('total do patrimônio desenhado no centro da rosca', () => {
    const { tela } = criar();
    const ctx = { save: vi.fn(), restore: vi.fn(), fillText: vi.fn() } as any;

    tela.centroTotalPlugin.afterDraw({ ctx, chartArea: undefined, data: { datasets: [] } });
    expect(ctx.fillText).not.toHaveBeenCalled();

    tela.centroTotalPlugin.afterDraw({
      ctx,
      chartArea: { left: 0, right: 100, top: 0, bottom: 100 },
      data: { datasets: [{ data: [600, 300, 'x'] }] },
    });
    expect(ctx.fillText).toHaveBeenCalledWith('Patrimônio', 50, 37);
    expect(ctx.fillText.mock.calls[1][0]).toContain('900');

    tela.centroTotalPlugin.afterDraw({ ctx, chartArea: { left: 0, right: 10, top: 0, bottom: 10 }, data: { datasets: [] } });
    expect(ctx.fillText).toHaveBeenCalledTimes(4);
  });

  it('no modo noturno os textos e grades dos gráficos ficam claros', () => {
    const { tela } = criar();
    const tema = TestBed.inject(TemaService);

    expect(tela.evolucaoOptions().scales.y.grid.color).toBe('#e3e2df');
    tema.tema.set('escuro');
    expect(tela.evolucaoOptions().scales.y.grid.color).toBe('#2c2f36');
    expect(tela.retornoOptions().scales.y.ticks.color).toBe('#e9e7e2');
    expect(tela.alocacaoData().datasets[0].borderColor).toBe('#1c1e24');
  });
});
