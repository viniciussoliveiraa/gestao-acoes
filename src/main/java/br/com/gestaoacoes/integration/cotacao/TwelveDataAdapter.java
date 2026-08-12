package br.com.gestaoacoes.integration.cotacao;

import br.com.gestaoacoes.exception.IntegracaoExternaIndisponivelException;
import br.com.gestaoacoes.exception.TickerNaoEncontradoException;
import br.com.gestaoacoes.model.Moeda;
import feign.FeignException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class TwelveDataAdapter implements CotacaoPort {

    private static final String PROVEDOR = "twelve-data";

    private final TwelveDataClient client;
    private final String apiKey;

    public TwelveDataAdapter(TwelveDataClient client, @Value("${app.integrations.twelve-data.api-key:}") String apiKey) {
        this.client = client;
        this.apiKey = apiKey;
    }

    @Override
    public CotacaoExterna obterCotacao(String tickerNormalizado) {
        TwelveDataResponse response;
        try {
            response = client.consultar(tickerNormalizado, apiKey);
        } catch (FeignException.BadRequest e) {
            throw new TickerNaoEncontradoException("Ticker não encontrado na Twelve Data: " + tickerNormalizado);
        } catch (RuntimeException e) {
            throw new IntegracaoExternaIndisponivelException("Falha ao consultar cotação na Twelve Data", e);
        }
        if (response.erro()) {
            throw new TickerNaoEncontradoException("Ticker não encontrado na Twelve Data: " + tickerNormalizado);
        }
        BigDecimal preco = new BigDecimal(response.close()).setScale(4, RoundingMode.HALF_UP);
        Moeda moeda = response.currency() == null ? Moeda.USD : Moeda.valueOf(response.currency());
        OffsetDateTime dataHora = response.timestamp() == null
                ? OffsetDateTime.now(ZoneOffset.UTC)
                : Instant.ofEpochSecond(response.timestamp()).atOffset(ZoneOffset.UTC);
        return new CotacaoExterna(response.name(), moeda, preco, dataHora, PROVEDOR);
    }
}