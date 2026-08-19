package br.com.gestaoacoes.integration.cotacao;

import br.com.gestaoacoes.exception.IntegracaoExternaIndisponivelException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class TwelveDataCambioAdapter implements CambioPort {

    private static final String SIMBOLO_USD_BRL = "USD/BRL";

    private final TwelveDataClient client;
    private final String apiKey;

    public TwelveDataCambioAdapter(TwelveDataClient client, @Value("${app.integrations.twelve-data.api-key:}") String apiKey) {
        this.client = client;
        this.apiKey = apiKey;
    }

    @Override
    public BigDecimal obterCotacaoUsdParaBrl() {
        TwelveDataResponse response;
        try {
            response = client.consultar(SIMBOLO_USD_BRL, apiKey);
        } catch (RuntimeException e) {
            throw new IntegracaoExternaIndisponivelException("Falha ao consultar câmbio USD/BRL na Twelve Data", e);
        }
        if (response.erro()) {
            throw new IntegracaoExternaIndisponivelException("Câmbio USD/BRL indisponível na Twelve Data");
        }
        return new BigDecimal(response.close()).setScale(4, RoundingMode.HALF_UP);
    }
}
