package br.com.gestaoacoes.integration.cotacao;

import br.com.gestaoacoes.model.Mercado;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CotacaoStrategyResolver {

    private final BrapiAdapter brapiAdapter;
    private final TwelveDataAdapter twelveDataAdapter;
    private final AlphaVantageAdapter alphaVantageAdapter;
    private final String usProvider;

    public CotacaoStrategyResolver(BrapiAdapter brapiAdapter,
                                    TwelveDataAdapter twelveDataAdapter,
                                    AlphaVantageAdapter alphaVantageAdapter,
                                    @Value("${app.market-data.us-provider:twelve-data}") String usProvider) {
        this.brapiAdapter = brapiAdapter;
        this.twelveDataAdapter = twelveDataAdapter;
        this.alphaVantageAdapter = alphaVantageAdapter;
        this.usProvider = usProvider;
    }

    public CotacaoPort resolver(Mercado mercado) {
        return switch (mercado) {
            case BRASIL -> brapiAdapter;
            case ESTADOS_UNIDOS -> "alpha-vantage".equalsIgnoreCase(usProvider) ? alphaVantageAdapter : twelveDataAdapter;
        };
    }
}