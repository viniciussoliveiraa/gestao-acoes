package br.com.gestaoacoes.integration.cotacao;

import br.com.gestaoacoes.exception.IntegracaoExternaIndisponivelException;
import br.com.gestaoacoes.exception.TickerNaoEncontradoException;
import br.com.gestaoacoes.model.Moeda;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class AlphaVantageAdapter implements CotacaoPort {

    private static final String PROVEDOR = "alpha-vantage";
    private static final String FUNCTION = "GLOBAL_QUOTE";

    private final AlphaVantageClient client;
    private final String apiKey;

    public AlphaVantageAdapter(AlphaVantageClient client, @Value("${app.integrations.alpha-vantage.api-key:}") String apiKey) {
        this.client = client;
        this.apiKey = apiKey;
    }

    @Override
    public CotacaoExterna obterCotacao(String tickerNormalizado) {
        AlphaVantageResponse response;
        try {
            response = client.consultar(FUNCTION, tickerNormalizado, apiKey);
        } catch (RuntimeException e) {
            throw new IntegracaoExternaIndisponivelException("Falha ao consultar cotação na Alpha Vantage", e);
        }
        if (response.limiteOuIndisponivel()) {
            throw new IntegracaoExternaIndisponivelException("Alpha Vantage indisponível ou limite de requisições excedido");
        }
        if (response.semCotacao()) {
            throw new TickerNaoEncontradoException("Ticker não encontrado na Alpha Vantage: " + tickerNormalizado);
        }
        AlphaVantageResponse.GlobalQuote quote = response.globalQuote();
        BigDecimal preco = new BigDecimal(quote.price()).setScale(4, RoundingMode.HALF_UP);
        OffsetDateTime dataHora = LocalDate.parse(quote.latestTradingDay()).atStartOfDay().atOffset(ZoneOffset.UTC);
        return new CotacaoExterna(null, Moeda.USD, preco, dataHora, PROVEDOR);
    }
}