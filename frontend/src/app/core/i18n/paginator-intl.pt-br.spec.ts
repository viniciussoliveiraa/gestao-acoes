import { criarPaginatorIntlPtBr } from './paginator-intl.pt-br';

describe('Paginador em português', () => {
  const intl = criarPaginatorIntlPtBr();

  it('traduz os rótulos dos botões', () => {
    expect(intl.itemsPerPageLabel).toBe('Itens por página:');
    expect(intl.nextPageLabel).toBe('Próxima página');
    expect(intl.previousPageLabel).toBe('Página anterior');
    expect(intl.firstPageLabel).toBe('Primeira página');
    expect(intl.lastPageLabel).toBe('Última página');
  });

  it('lista vazia mostra "0 de 0"', () => {
    expect(intl.getRangeLabel(0, 10, 0)).toBe('0 de 0');
  });

  it('tamanho de página zero mostra "0 de N"', () => {
    expect(intl.getRangeLabel(0, 0, 25)).toBe('0 de 25');
  });

  it('primeira página cheia', () => {
    expect(intl.getRangeLabel(0, 10, 25)).toBe('1 – 10 de 25');
  });

  it('última página incompleta para no total', () => {
    expect(intl.getRangeLabel(2, 10, 25)).toBe('21 – 25 de 25');
  });

  it('página além do total não quebra', () => {
    expect(intl.getRangeLabel(5, 10, 25)).toBe('51 – 60 de 25');
  });
});
