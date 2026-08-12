package br.com.gestaoacoes.integration.cotacao;

import br.com.gestaoacoes.model.Mercado;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

class CotacaoStrategyResolverTest {

    private final BrapiAdapter brapiAdapter = Mockito.mock(BrapiAdapter.class);
    private final TwelveDataAdapter twelveDataAdapter = Mockito.mock(TwelveDataAdapter.class);
    private final AlphaVantageAdapter alphaVantageAdapter = Mockito.mock(AlphaVantageAdapter.class);

    @Test
    void mercadoBrasilSempreResolveParaBrapi() {
        CotacaoStrategyResolver resolver = new CotacaoStrategyResolver(
                brapiAdapter, twelveDataAdapter, alphaVantageAdapter, "twelve-data");

        assertThat(resolver.resolver(Mercado.BRASIL)).isSameAs(brapiAdapter);
    }

    @Test
    void mercadoEstadosUnidosResolveParaTwelveDataPorPadrao() {
        CotacaoStrategyResolver resolver = new CotacaoStrategyResolver(
                brapiAdapter, twelveDataAdapter, alphaVantageAdapter, "twelve-data");

        assertThat(resolver.resolver(Mercado.ESTADOS_UNIDOS)).isSameAs(twelveDataAdapter);
    }

    @Test
    void mercadoEstadosUnidosResolveParaAlphaVantageQuandoConfigurado() {
        CotacaoStrategyResolver resolver = new CotacaoStrategyResolver(
                brapiAdapter, twelveDataAdapter, alphaVantageAdapter, "alpha-vantage");

        assertThat(resolver.resolver(Mercado.ESTADOS_UNIDOS)).isSameAs(alphaVantageAdapter);
    }
}