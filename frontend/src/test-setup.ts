/**
 * Preparação dos testes do frontend (roda antes de cada arquivo .spec.ts).
 *
 * O ambiente de teste (jsdom) não implementa window.matchMedia, que as telas usam para saber
 * se o usuário prefere menos animação e o modo noturno usa para seguir o sistema. Aqui ele é
 * simulado; cada teste pode trocar o valor com `simularPreferencias` (core/testing/dados.ts).
 */
if (!window.matchMedia) {
  Object.defineProperty(window, 'matchMedia', {
    writable: true,
    configurable: true,
    value: (query: string) => ({
      matches: false,
      media: query,
      onchange: null,
      addListener: () => undefined,
      removeListener: () => undefined,
      addEventListener: () => undefined,
      removeEventListener: () => undefined,
      dispatchEvent: () => false,
    }),
  });
}
