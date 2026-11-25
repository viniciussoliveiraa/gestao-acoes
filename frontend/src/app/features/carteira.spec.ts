import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { CarteiraService } from '../core/services/carteira.service';
import { ProventoService } from '../core/services/provento.service';
import { erroHttp, pagina, posicao, provento, simularMenosMovimento } from '../core/testing/dados';
import { Rebalanceamento } from './rebalanceamento/rebalanceamento';
import { Resumo } from './resumo/resumo';

/* eslint-disable @typescript-eslint/no-explicit-any */

const petr = posicao({ acaoId: 1, ticker: 'PETR4', nomeEmpresa: 'Petrobras', valorInvestido: 300, valorAtual: 600, variacaoPercentual: 100 });
const vale = posicao({ acaoId: 2, ticker: 'VALE3', nomeEmpresa: 'Vale', valorInvestido: 400, valorAtual: 300, variacaoPercentual: -25, resultadoRealizado: 50 });
const itub = posicao({ acaoId: 3, ticker: 'ITUB4', nomeEmpresa: null, valorInvestido: 100, valorAtual: 100, variacaoPercentual: 0 });

describe('Tela Resumo', () => {
  const matchMediaOriginal = window.matchMedia;

  function criar(posicoes = [petr, vale, itub], proventos = [provento({ valorTotal: 10 }), provento({ id: 2, valorTotal: 5 })]) {
    TestBed.configureTestingModule({
      imports: [Resumo],
      providers: [
        provideRouter([]),
        { provide: CarteiraService, useValue: { listarPosicoes: vi.fn(() => of(posicoes)) } },
        { provide: ProventoService, useValue: { listar: vi.fn(() => of(pagina(proventos))) } },
      ],
    });
    const fixture = TestBed.createComponent(Resumo);
    fixture.detectChanges();
    TestBed.tick();
    fixture.detectChanges();
    return { fixture, tela: fixture.componentInstance as any, texto: () => fixture.nativeElement.textContent as string };
  }

  beforeEach(() => simularMenosMovimento(true));
  afterEach(() => {
    window.matchMedia = matchMediaOriginal;
    vi.useRealTimers();
    vi.restoreAllMocks();
  });

  it('calcula totais, variação e o peso de cada ativo na carteira', () => {
    const { tela, texto } = criar();

    expect(tela.valorTotalInvestido).toBe(800);
    expect(tela.valorTotalAtual).toBe(1000);
    expect(tela.variacaoTotal).toBe(25);
    expect(tela.valorTotalProventos()).toBe(15);
    expect(tela.linhasTabela().map((l: any) => l.percentualCarteira)).toEqual([60, 30, 10]);
    expect(texto()).toContain('PETR4');
  });

  it('com "menos animação" os números já aparecem com o valor final', () => {
    const { tela } = criar();

    expect(tela.valorInvestidoAnimado()).toBe(800);
    expect(tela.valorAtualAnimado()).toBe(1000);
    expect(tela.valorProventosAnimado()).toBe(15);
  });

  it('com animação os números sobem até o valor final', () => {
    simularMenosMovimento(false);
    // Os quadros da animação ficam numa fila e são disparados à mão pelo teste.
    const quadros: FrameRequestCallback[] = [];
    vi.spyOn(window, 'requestAnimationFrame').mockImplementation((cb) => quadros.push(cb));
    const inicio = performance.now();
    const { tela } = criar();

    quadros.splice(0).forEach((cb) => cb(inicio + 450)); // meio da animação
    expect(tela.valorAtualAnimado()).toBeGreaterThan(0);
    expect(tela.valorAtualAnimado()).toBeLessThan(1000);

    quadros.splice(0).forEach((cb) => cb(inicio + 5000)); // fim
    expect(tela.valorAtualAnimado()).toBe(1000);
    expect(tela.valorProventosAnimado()).toBe(15);
    expect(quadros.length).toBe(0);
  });

  it('melhor e pior ativo pela variação', () => {
    const { tela } = criar();

    expect(tela.melhorAtivo().ticker).toBe('PETR4');
    expect(tela.piorAtivo().ticker).toBe('VALE3');
  });

  it('avisa concentração quando um ativo passa de 40% da carteira', () => {
    expect(criar().tela.alertaConcentracao().ticker).toBe('PETR4');
  });

  it('carteira equilibrada ou com um ativo só não gera aviso de concentração', () => {
    const equilibrada = [
      posicao({ ticker: 'A', valorAtual: 100 }),
      posicao({ ticker: 'B', valorAtual: 100 }),
      posicao({ ticker: 'C', valorAtual: 100 }),
    ];
    expect(criar(equilibrada).tela.alertaConcentracao()).toBeNull();
    TestBed.resetTestingModule();
    expect(criar([petr]).tela.alertaConcentracao()).toBeNull();
  });

  it('carteira vazia: tudo zero, sem divisão por zero', () => {
    const { tela } = criar([], []);

    expect(tela.variacaoTotal).toBe(0);
    expect(tela.melhorAtivo()).toBeNull();
    expect(tela.valorInvestidoAnimado()).toBe(0);
  });

  it('carteira com valor atual zero não divide por zero no peso', () => {
    const { tela } = criar([posicao({ valorAtual: 0 }), posicao({ ticker: 'X', valorAtual: 0 })]);

    expect(tela.linhasTabela().every((l: any) => l.percentualCarteira === 0)).toBe(true);
  });

  it('busca por ticker ou nome da empresa', () => {
    const { tela } = criar();

    tela.onFiltroChange({ target: { value: 'vale' } } as unknown as Event);
    expect(tela.linhasTabela().map((l: any) => l.ticker)).toEqual(['VALE3']);

    tela.onFiltroChange({ target: { value: 'itub' } } as unknown as Event);
    expect(tela.linhasTabela().map((l: any) => l.ticker)).toEqual(['ITUB4']);

    tela.onFiltroChange({ target: { value: '   ' } } as unknown as Event);
    expect(tela.linhasTabela().length).toBe(3);
  });

  it('ordena por texto, por número, crescente, decrescente ou sem ordem', () => {
    const { tela } = criar();

    tela.onOrdenacaoChange({ active: 'ticker', direction: 'asc' });
    expect(tela.linhasTabela().map((l: any) => l.ticker)).toEqual(['ITUB4', 'PETR4', 'VALE3']);

    tela.onOrdenacaoChange({ active: 'variacaoPercentual', direction: 'asc' });
    expect(tela.linhasTabela().map((l: any) => l.ticker)).toEqual(['VALE3', 'ITUB4', 'PETR4']);

    tela.onOrdenacaoChange({ active: 'valorAtual', direction: '' });
    expect(tela.linhasTabela().map((l: any) => l.ticker)).toEqual(['PETR4', 'VALE3', 'ITUB4']);
  });

  it('erro ao carregar a carteira aparece na tela', () => {
    TestBed.configureTestingModule({
      imports: [Resumo],
      providers: [
        provideRouter([]),
        { provide: CarteiraService, useValue: { listarPosicoes: () => erroHttp(500, 'Falha ao carregar posições') } },
        { provide: ProventoService, useValue: { listar: () => of(pagina([])) } },
      ],
    });
    const fixture = TestBed.createComponent(Resumo);
    fixture.detectChanges();

    expect((fixture.componentInstance as any).erro()).toBe('Falha ao carregar posições');
    expect(fixture.nativeElement.textContent).toContain('Falha ao carregar posições');
  });

  it('exportar CSV gera o arquivo com ; e vírgula decimal (padrão do Excel em pt-BR)', async () => {
    const { tela } = criar();
    let blobGerado: Blob | undefined;
    URL.createObjectURL = vi.fn((blob: Blob) => {
      blobGerado = blob;
      return 'blob:teste';
    });
    URL.revokeObjectURL = vi.fn();
    const clicar = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);

    tela.exportarCsv();

    expect(clicar).toHaveBeenCalled();
    const conteudo = await blobGerado!.text();
    expect(conteudo).toContain('"Ativo";"Empresa"');
    expect(conteudo).toContain('"PETR4";"Petrobras";"10";"30,00";"300,00";"600,00";"60,00%";"100,00%"');
    expect(conteudo).toContain('"ITUB4";""');
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:teste');
  });
});

describe('Tela Reequilíbrio (rebalanceamento)', () => {
  const CHAVE = 'gestao-acoes:metas-alocacao';
  let listarPosicoes: ReturnType<typeof vi.fn>;

  function criar(posicoes = [petr, vale, itub]) {
    listarPosicoes = vi.fn(() => of(posicoes));
    TestBed.configureTestingModule({
      imports: [Rebalanceamento],
      providers: [provideRouter([]), { provide: CarteiraService, useValue: { listarPosicoes } }],
    });
    const fixture = TestBed.createComponent(Rebalanceamento);
    fixture.detectChanges();
    return { fixture, tela: fixture.componentInstance as any, texto: () => fixture.nativeElement.textContent as string };
  }

  const linha = (tela: any, ticker: string) => tela.linhas().find((l: any) => l.ticker === ticker);

  beforeEach(() => localStorage.clear());
  afterEach(() => vi.restoreAllMocks());

  it('sem metas salvas começa com a alocação de hoje (nada a comprar)', () => {
    const { tela, texto } = criar();

    expect(tela.metas()).toEqual({ PETR4: 60, VALE3: 30, ITUB4: 10 });
    expect(tela.metasEquilibradas()).toBe(true);
    expect(JSON.parse(localStorage.getItem(CHAVE)!)).toEqual({ PETR4: 60, VALE3: 30, ITUB4: 10 });
    expect(tela.totalResultadoRealizado()).toBe(50);
    expect(texto()).toContain('PETR4');
  });

  it('metas salvas no navegador são reaproveitadas; só faltantes são completadas', () => {
    localStorage.setItem(CHAVE, JSON.stringify({ PETR4: 50, VALE3: 50 }));

    const { tela } = criar();

    expect(tela.metas()).toEqual({ PETR4: 50, VALE3: 50, ITUB4: 10 });
  });

  it('metas salvas completas não são regravadas', () => {
    localStorage.setItem(CHAVE, JSON.stringify({ PETR4: 40, VALE3: 40, ITUB4: 20 }));
    const gravar = vi.spyOn(Storage.prototype, 'setItem');

    criar();

    expect(gravar).not.toHaveBeenCalled();
  });

  it('conteúdo salvo corrompido é ignorado', () => {
    localStorage.setItem(CHAVE, '{nao-e-json');

    expect(criar().tela.metas()).toEqual({ PETR4: 60, VALE3: 30, ITUB4: 10 });
  });

  it('armazenamento bloqueado não quebra a tela', () => {
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('bloqueado');
    });
    const { tela } = criar();

    tela.atualizarMeta('PETR4', 70);

    expect(tela.metas().PETR4).toBe(70);
  });

  it('aporte vai primeiro para quem está abaixo da meta, sem passar do alvo', () => {
    const { tela } = criar();
    tela.atualizarMeta('PETR4', 50);
    tela.atualizarMeta('VALE3', 40);
    tela.atualizarMeta('ITUB4', 10);

    tela.atualizarAporte(100);

    // Base 1100: alvo PETR4 550 (acima, recebe 0), VALE3 440 (faltam 140), ITUB4 110 (faltam 10).
    expect(linha(tela, 'PETR4').sugestaoAporte).toBe(0);
    expect(linha(tela, 'ITUB4').sugestaoAporte).toBeCloseTo(10);
    expect(linha(tela, 'VALE3').sugestaoAporte).toBeCloseTo(90);
    expect(tela.totalSugerido()).toBeCloseTo(100);
    expect(tela.aporteNaoAlocado()).toBeCloseTo(0);
  });

  it('aporte maior que o necessário sobra como "não alocado"', () => {
    const { tela } = criar();

    tela.atualizarAporte(1000);

    // Metas = alocação de hoje: base 2000, cada um cabe exatamente o dobro do atual.
    expect(tela.totalSugerido()).toBeCloseTo(1000);
    tela.atualizarMeta('PETR4', 0);
    tela.atualizarMeta('VALE3', 0);
    tela.atualizarMeta('ITUB4', 0);
    expect(tela.aporteNaoAlocado()).toBe(1000);
  });

  it('valores inválidos (negativo, NaN) viram zero', () => {
    const { tela } = criar();

    tela.atualizarMeta('PETR4', -5);
    tela.atualizarAporte(Number.NaN);

    expect(tela.metas().PETR4).toBe(0);
    expect(tela.aporte()).toBe(0);
    expect(tela.metasEquilibradas()).toBe(false);
  });

  it('botões "como hoje" e "igualmente"', () => {
    const { tela } = criar();

    tela.distribuirIgualmente();
    expect(tela.metas()).toEqual({ PETR4: 33.33, VALE3: 33.33, ITUB4: 33.33 });

    tela.distribuirComoHoje();
    expect(tela.metas()).toEqual({ PETR4: 60, VALE3: 30, ITUB4: 10 });
  });

  it('carteira vazia: botões não quebram e nada é sugerido', () => {
    const { tela } = criar([]);

    tela.distribuirIgualmente();
    tela.distribuirComoHoje();
    tela.atualizarAporte(100);

    expect(tela.metas()).toEqual({});
    expect(tela.aporteNaoAlocado()).toBe(100);
  });

  it('carteira com valor zero usa meta zero', () => {
    const { tela } = criar([posicao({ ticker: 'A', valorAtual: 0 })]);

    expect(tela.metas()).toEqual({ A: 0 });
    tela.distribuirComoHoje();
    expect(tela.metas()).toEqual({ A: 0 });
    expect(linha(tela, 'A').percentualAtual).toBe(0);
  });

  it('erro ao carregar posições aparece na tela', () => {
    TestBed.configureTestingModule({
      imports: [Rebalanceamento],
      providers: [
        provideRouter([]),
        { provide: CarteiraService, useValue: { listarPosicoes: () => erroHttp(500, 'Falha ao carregar posições') } },
      ],
    });
    const fixture = TestBed.createComponent(Rebalanceamento);
    fixture.detectChanges();

    expect((fixture.componentInstance as any).erro()).toBe('Falha ao carregar posições');
  });
});
