import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { ConsultaCnpj } from '../core/models/corretora.model';
import { AcaoService } from '../core/services/acao.service';
import { CarteiraService } from '../core/services/carteira.service';
import { CorretoraService } from '../core/services/corretora.service';
import { ProventoService } from '../core/services/provento.service';
import { acao, corretora, erroHttp, lancamento, pagina, provento } from '../core/testing/dados';
import { Acoes } from './acoes/acoes';
import { Corretoras } from './corretoras/corretoras';
import { Lancamentos } from './lancamentos/lancamentos';
import { Proventos } from './proventos/proventos';

/* eslint-disable @typescript-eslint/no-explicit-any */

function renderizar<T>(componente: new (...args: any[]) => T, providers: unknown[]) {
  TestBed.configureTestingModule({ imports: [componente], providers: providers as any[] });
  const fixture = TestBed.createComponent(componente);
  fixture.detectChanges();
  return { fixture, tela: fixture.componentInstance as any, texto: () => fixture.nativeElement.textContent as string };
}

describe('Tela Corretoras', () => {
  let servico: {
    listar: ReturnType<typeof vi.fn>;
    cadastrar: ReturnType<typeof vi.fn>;
    consultarCnpj: ReturnType<typeof vi.fn>;
  };

  const dadosXp: ConsultaCnpj = {
    cnpj: '02332886000104',
    razaoSocial: 'XP INVESTIMENTOS CCTVM S.A.',
    nomeFantasia: 'XP',
    situacaoCadastral: 'ATIVA',
    email: 'contato@xp.com.br',
    telefone: '1133331234',
    cep: '04538133',
    logradouro: 'AVENIDA BRIGADEIRO FARIA LIMA',
    numero: '3600',
    complemento: '10 ANDAR',
    bairro: 'ITAIM BIBI',
    cidade: 'SAO PAULO',
    uf: 'SP',
    validadaCvm: true,
  };

  function criar(lista = [corretora(), corretora({ id: 2, razaoSocial: 'Genial', uf: 'RJ', validadaCvm: false })]) {
    servico = {
      listar: vi.fn(() => of(pagina(lista))),
      cadastrar: vi.fn(() => of(corretora({ razaoSocial: 'Nova Corretora' }))),
      consultarCnpj: vi.fn(() => of(dadosXp)),
    };
    return renderizar(Corretoras, [{ provide: CorretoraService, useValue: servico }]);
  }

  it('lista as corretoras com os totais (cadastradas, validadas na CVM, estados)', () => {
    const { tela, texto } = criar();

    expect(tela.totalCorretoras()).toBe(2);
    expect(tela.validadasCvm()).toBe(1);
    expect(tela.estadosAtendidos()).toBe(2);
    expect(texto()).toContain('XP Investimentos');
    expect(texto()).toContain('Genial');
  });

  it('sem corretoras mostra a mensagem de lista vazia', () => {
    expect(criar([]).texto()).toContain('Nenhuma corretora cadastrada');
  });

  it('formulário sem CNPJ/CEP não chama a API', () => {
    const { tela } = criar();

    tela.submeter();

    expect(servico.cadastrar).not.toHaveBeenCalled();
  });

  it('cadastro certo envia campos opcionais vazios como null e recarrega a lista', () => {
    const { fixture, tela, texto } = criar();
    // Consulta do CNPJ sem dados opcionais: nada é preenchido sozinho.
    servico.consultarCnpj.mockReturnValue(
      of({ ...dadosXp, cep: null, numero: null, complemento: null, email: null, telefone: null })
    );
    tela.form.patchValue({ cnpj: '02332886000104', cep: '04538133' });

    tela.submeter();
    fixture.detectChanges();

    expect(servico.cadastrar).toHaveBeenCalledWith({
      cnpj: '02332886000104',
      cep: '04538133',
      numero: null,
      complemento: null,
      email: null,
      telefone: null,
    });
    expect(texto()).toContain('Corretora "Nova Corretora" cadastrada com sucesso.');
    expect(servico.listar).toHaveBeenCalledTimes(2);
  });

  it('cadastro com todos os campos preenchidos envia os valores', () => {
    const { tela } = criar();
    tela.form.setValue({
      cnpj: '02332886000104',
      cep: '04538133',
      numero: '10',
      complemento: 'sala 1',
      email: 'xp@exemplo.com',
      telefone: '1199999999',
    });

    tela.submeter();

    expect(servico.cadastrar).toHaveBeenCalledWith(expect.objectContaining({ numero: '10', telefone: '1199999999' }));
  });

  describe('preenchimento automático pelo CNPJ', () => {
    it('enquanto o CNPJ está incompleto não consulta nada', () => {
      const { tela } = criar();

      tela.form.controls.cnpj.setValue('02.332.886/0001');

      expect(servico.consultarCnpj).not.toHaveBeenCalled();
      expect(tela.consulta()).toBeNull();
    });

    it('com os 14 dígitos (com ou sem máscara) consulta e preenche os outros campos', () => {
      const { fixture, tela, texto } = criar();

      tela.form.controls.cnpj.setValue('02.332.886/0001-04');
      fixture.detectChanges();

      expect(servico.consultarCnpj).toHaveBeenCalledWith('02332886000104');
      expect(tela.form.getRawValue()).toEqual({
        cnpj: '02.332.886/0001-04',
        cep: '04538133',
        numero: '3600',
        complemento: '10 ANDAR',
        email: 'contato@xp.com.br',
        telefone: '1133331234',
      });
      expect(tela.consultando()).toBe(false);
      expect(texto()).toContain('XP INVESTIMENTOS CCTVM S.A.');
      expect(texto()).toContain('AVENIDA BRIGADEIRO FARIA LIMA, 3600');
      expect(texto()).toContain('Em funcionamento na CVM');
    });

    it('não consulta de novo se só a máscara mudou', () => {
      const { tela } = criar();

      tela.form.controls.cnpj.setValue('02332886000104');
      tela.form.controls.cnpj.setValue('02.332.886/0001-04');

      expect(servico.consultarCnpj).toHaveBeenCalledTimes(1);
    });

    it('campos que o CNPJ não informa ficam como o usuário digitou', () => {
      const { fixture, tela, texto } = criar();
      servico.consultarCnpj.mockReturnValue(
        of({ ...dadosXp, email: null, telefone: null, complemento: null, logradouro: null, nomeFantasia: null, situacaoCadastral: null })
      );
      tela.form.controls.email.setValue('meu@email.com');

      tela.form.controls.cnpj.setValue('02332886000104');
      fixture.detectChanges();

      expect(tela.form.controls.email.value).toBe('meu@email.com');
      expect(tela.form.controls.cep.value).toBe('04538133');
      expect(texto()).not.toContain('Receita:');
    });

    it('CNPJ fora da CVM mostra o aviso de que o cadastro será recusado', () => {
      const { fixture, tela, texto } = criar();
      servico.consultarCnpj.mockReturnValue(of({ ...dadosXp, validadaCvm: false }));

      tela.form.controls.cnpj.setValue('02332886000104');
      fixture.detectChanges();

      expect(texto()).toContain('Não consta na CVM: o cadastro será recusado');
    });

    it('CNPJ já cadastrado (409) mostra o erro e não preenche nada', () => {
      const { fixture, tela, texto } = criar();
      servico.consultarCnpj.mockReturnValue(erroHttp(409, 'Já existe uma corretora cadastrada com este CNPJ'));

      tela.form.controls.cnpj.setValue('02332886000104');
      fixture.detectChanges();

      expect(texto()).toContain('Já existe uma corretora cadastrada com este CNPJ');
      expect(tela.consulta()).toBeNull();
      expect(tela.consultando()).toBe(false);
      expect(tela.form.controls.cep.value).toBe('');
    });

    it('apagar um dígito do CNPJ limpa o quadro e o erro', () => {
      const { tela } = criar();
      tela.form.controls.cnpj.setValue('02332886000104');
      expect(tela.consulta()).not.toBeNull();

      tela.form.controls.cnpj.setValue('0233288600010');

      expect(tela.consulta()).toBeNull();
      expect(tela.erroConsulta()).toBeNull();
    });
  });

  it('erro do backend (ex.: 403 sem ser admin) aparece na tela', () => {
    const { fixture, tela, texto } = criar();
    servico.cadastrar.mockReturnValue(erroHttp(403, 'Acesso negado'));
    tela.form.patchValue({ cnpj: '1', cep: '2' });

    tela.submeter();
    fixture.detectChanges();

    expect(texto()).toContain('Acesso negado');
    expect(tela.enviando()).toBe(false);
  });
});

describe('Tela Ações', () => {
  let servico: {
    listar: ReturnType<typeof vi.fn>;
    cadastrar: ReturnType<typeof vi.fn>;
    atualizarCotacao: ReturnType<typeof vi.fn>;
  };

  function criar(lista = [acao(), acao({ id: 2, ticker: 'AAPL', mercado: 'ESTADOS_UNIDOS', moeda: 'USD' })]) {
    servico = {
      listar: vi.fn(() => of(pagina(lista))),
      cadastrar: vi.fn(() => of(acao({ ticker: 'VALE3' }))),
      atualizarCotacao: vi.fn(() => of(acao())),
    };
    return renderizar(Acoes, [{ provide: AcaoService, useValue: servico }]);
  }

  it('lista as ações e conta por mercado', () => {
    const { tela, texto } = criar();

    expect(tela.totalAcoes()).toBe(2);
    expect(tela.totalBrasil()).toBe(1);
    expect(tela.totalEstadosUnidos()).toBe(1);
    expect(texto()).toContain('PETR4');
    expect(texto()).toContain('AAPL');
  });

  it('sem ações mostra a lista vazia', () => {
    expect(criar([]).tela.totalAcoes()).toBe(0);
  });

  it('ticker vazio não chama a API', () => {
    const { tela } = criar();

    tela.submeter();

    expect(servico.cadastrar).not.toHaveBeenCalled();
  });

  it('cadastro certo mostra sucesso e volta o mercado para BRASIL', () => {
    const { fixture, tela, texto } = criar();
    tela.form.setValue({ ticker: 'VALE3', mercado: 'ESTADOS_UNIDOS' });

    tela.submeter();
    fixture.detectChanges();

    expect(servico.cadastrar).toHaveBeenCalledWith({ ticker: 'VALE3', mercado: 'ESTADOS_UNIDOS' });
    expect(texto()).toContain('Ação VALE3 cadastrada com sucesso.');
    expect(tela.form.controls.mercado.value).toBe('BRASIL');
  });

  it('ticker inexistente mostra o erro do backend', () => {
    const { tela } = criar();
    servico.cadastrar.mockReturnValue(erroHttp(404, 'Ticker não encontrado'));
    tela.form.setValue({ ticker: 'XXXX9', mercado: 'BRASIL' });

    tela.submeter();

    expect(tela.erro()).toBe('Ticker não encontrado');
  });

  it('atualizar cotação recarrega a lista', () => {
    const { tela } = criar();

    tela.atualizarCotacao(acao({ id: 7 }));

    expect(servico.atualizarCotacao).toHaveBeenCalledWith(7);
    expect(tela.atualizandoId()).toBeNull();
    expect(servico.listar).toHaveBeenCalledTimes(2);
  });

  it('falha ao atualizar cotação mostra o erro', () => {
    const { tela } = criar();
    servico.atualizarCotacao.mockReturnValue(erroHttp(503, 'Provedor de cotação indisponível'));

    tela.atualizarCotacao(acao());

    expect(tela.erro()).toBe('Provedor de cotação indisponível');
    expect(tela.atualizandoId()).toBeNull();
  });
});

describe('Tela Lançamentos', () => {
  let carteira: {
    listarLancamentos: ReturnType<typeof vi.fn>;
    registrarLancamento: ReturnType<typeof vi.fn>;
    excluirLancamento: ReturnType<typeof vi.fn>;
  };
  const lista = [
    lancamento({ id: 1, tipo: 'COMPRA', quantidade: 10, precoUnitario: 30 }),
    lancamento({ id: 2, tipo: 'COMPRA', quantidade: 5, precoUnitario: 40 }),
    lancamento({ id: 3, tipo: 'VENDA', quantidade: 2, precoUnitario: 50 }),
  ];

  function criar(lancamentos = lista) {
    carteira = {
      listarLancamentos: vi.fn(() => of(pagina(lancamentos))),
      registrarLancamento: vi.fn(() => of(lancamento())),
      excluirLancamento: vi.fn(() => of(undefined)),
    };
    return renderizar(Lancamentos, [
      { provide: CarteiraService, useValue: carteira },
      { provide: AcaoService, useValue: { listar: vi.fn(() => of(pagina([acao({ id: 1, cotacaoAtual: 38.5 })]))) } },
      { provide: CorretoraService, useValue: { listar: vi.fn(() => of(pagina([corretora()]))) } },
    ]);
  }

  afterEach(() => vi.restoreAllMocks());

  it('calcula total investido (só compras), quantidade e ticket médio', () => {
    const { tela, texto } = criar();

    expect(tela.totalInvestido()).toBe(500);
    expect(tela.quantidadeLancamentos()).toBe(3);
    expect(tela.ticketMedio()).toBe(250);
    expect(texto()).toContain('PETR4');
  });

  it('sem compras o ticket médio é zero', () => {
    expect(criar([]).tela.ticketMedio()).toBe(0);
  });

  it('escolher a ação preenche o preço com a cotação atual; ação desconhecida não mexe', () => {
    const { tela } = criar();

    tela.form.controls.acaoId.setValue(1);
    expect(tela.form.controls.precoUnitario.value).toBe(38.5);

    tela.form.controls.precoUnitario.setValue(10);
    tela.form.controls.acaoId.setValue(999);
    expect(tela.form.controls.precoUnitario.value).toBe(10);
  });

  it('trocar de página recarrega com a página e o tamanho escolhidos', () => {
    const { tela } = criar();

    tela.onPageChange({ pageIndex: 2, pageSize: 25, length: 100 });

    expect(carteira.listarLancamentos).toHaveBeenLastCalledWith(2, 25);
  });

  it('formulário incompleto não chama a API', () => {
    const { tela } = criar();

    tela.submeter();

    expect(carteira.registrarLancamento).not.toHaveBeenCalled();
  });

  it('registrar envia a data no formato AAAA-MM-DD e mostra sucesso', () => {
    const { tela } = criar();
    tela.form.setValue({
      acaoId: 1,
      corretoraId: 1,
      tipo: 'COMPRA',
      quantidade: 10,
      precoUnitario: 30,
      dataOperacao: new Date(2026, 0, 5),
    });

    tela.submeter();

    expect(carteira.registrarLancamento).toHaveBeenCalledWith(expect.objectContaining({ dataOperacao: '2026-01-05' }));
    expect(tela.sucesso()).toBe('Lançamento registrado com sucesso.');
    expect(tela.pageIndex()).toBe(0);
  });

  it('venda acima do saldo mostra o erro do backend (422)', () => {
    const { tela } = criar();
    carteira.registrarLancamento.mockReturnValue(erroHttp(422, 'Quantidade acima do saldo disponível'));
    tela.form.setValue({
      acaoId: 1,
      corretoraId: 1,
      tipo: 'VENDA',
      quantidade: 1000,
      precoUnitario: 30,
      dataOperacao: new Date(2026, 10, 25),
    });

    tela.submeter();

    expect(tela.erro()).toBe('Quantidade acima do saldo disponível');
    expect(tela.enviando()).toBe(false);
  });

  it('excluir pede confirmação; cancelando não chama a API', () => {
    const { tela } = criar();
    const confirmar = vi.spyOn(window, 'confirm').mockReturnValue(false);

    tela.excluir(lista[2]);

    expect(confirmar.mock.calls[0][0]).toContain('venda de 2 PETR4');
    expect(carteira.excluirLancamento).not.toHaveBeenCalled();
  });

  it('excluir confirmado chama a API e mostra sucesso', () => {
    const { tela } = criar();
    const confirmar = vi.spyOn(window, 'confirm').mockReturnValue(true);

    tela.excluir(lista[0]);

    expect(confirmar.mock.calls[0][0]).toContain('compra de 10 PETR4');
    expect(carteira.excluirLancamento).toHaveBeenCalledWith(1);
    expect(tela.sucesso()).toBe('Lançamento excluído com sucesso.');
    expect(tela.excluindoId()).toBeNull();
  });

  it('falha ao excluir mostra o erro', () => {
    const { tela } = criar();
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    carteira.excluirLancamento.mockReturnValue(erroHttp(404, 'Lançamento não encontrado'));

    tela.excluir(lista[0]);

    expect(tela.erro()).toBe('Lançamento não encontrado');
    expect(tela.excluindoId()).toBeNull();
  });
});

describe('Tela Proventos', () => {
  let servico: { listar: ReturnType<typeof vi.fn>; registrar: ReturnType<typeof vi.fn> };
  const lista = [
    provento({ id: 1, tipo: 'DIVIDENDO', valorTotal: 10 }),
    provento({ id: 2, tipo: 'JCP', valorTotal: 5 }),
    provento({ id: 3, tipo: 'DIVIDENDO', valorTotal: 2.5 }),
  ];

  function criar() {
    servico = { listar: vi.fn(() => of(pagina(lista))), registrar: vi.fn(() => of(provento())) };
    return renderizar(Proventos, [
      { provide: ProventoService, useValue: servico },
      { provide: AcaoService, useValue: { listar: vi.fn(() => of(pagina([acao()]))) } },
    ]);
  }

  it('soma o total recebido, dividendos e JCP', () => {
    const { tela, texto } = criar();

    expect(tela.totalRecebido()).toBe(17.5);
    expect(tela.totalDividendos()).toBe(12.5);
    expect(tela.totalJcp()).toBe(5);
    expect(texto()).toContain('PETR4');
  });

  it('trocar de página recarrega com a página escolhida', () => {
    const { tela } = criar();

    tela.onPageChange({ pageIndex: 1, pageSize: 5, length: 20 });

    expect(servico.listar).toHaveBeenLastCalledWith(1, 5);
  });

  it('formulário incompleto não chama a API', () => {
    const { tela } = criar();

    tela.submeter();

    expect(servico.registrar).not.toHaveBeenCalled();
  });

  it('registrar envia a data AAAA-MM-DD, mostra sucesso e volta o tipo para DIVIDENDO', () => {
    const { tela } = criar();
    tela.form.setValue({ acaoId: 1, tipo: 'JCP', valorTotal: 20, dataPagamento: new Date(2026, 10, 9) });

    tela.submeter();

    expect(servico.registrar).toHaveBeenCalledWith({ acaoId: 1, tipo: 'JCP', valorTotal: 20, dataPagamento: '2026-11-09' });
    expect(tela.sucesso()).toBe('Provento registrado com sucesso.');
    expect(tela.form.controls.tipo.value).toBe('DIVIDENDO');
  });

  it('erro do backend aparece na tela', () => {
    const { tela } = criar();
    servico.registrar.mockReturnValue(erroHttp(400, 'valorTotal deve ser positivo'));
    tela.form.setValue({ acaoId: 1, tipo: 'DIVIDENDO', valorTotal: 1, dataPagamento: new Date() });

    tela.submeter();

    expect(tela.erro()).toBe('valorTotal deve ser positivo');
    expect(tela.enviando()).toBe(false);
  });
});
