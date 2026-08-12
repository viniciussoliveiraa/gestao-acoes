package br.com.gestaoacoes.integration.cotacao;

import br.com.gestaoacoes.exception.IntegracaoExternaIndisponivelException;
import br.com.gestaoacoes.exception.TickerNaoEncontradoException;
import br.com.gestaoacoes.model.Moeda;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class BrapiAdapter implements CotacaoPort {

    private static final String PROVEDOR = "brapi";

    private final BrapiClient client;
    private final String token;

    public BrapiAdapter(BrapiClient client, @Value("${app.integrations.brapi.token:}") String token) {
        this.client = client;
        this.token = token;
    }

    @Override
    public CotacaoExterna obterCotacao(String tickerNormalizado) {
        BrapiResponse response;
        try {
            String authorization = token == null || token.isBlank() ? null : "Bearer " + token;
            response = client.consultar(tickerNormalizado, authorization);
        } catch (RuntimeException e) {
            throw new IntegracaoExternaIndisponivelException("Falha ao consultar cotação na brapi.dev", e);
        }
        if (response.results() == null || response.results().isEmpty()) {
            throw new TickerNaoEncontradoException("Ticker não encontrado na brapi.dev: " + tickerNormalizado);
        }
        BrapiResponse.BrapiData data = response.results().get(0).data();
        BigDecimal preco = data.regularMarketPrice().setScale(4, RoundingMode.HALF_UP);
        Moeda moeda = "BRL".equalsIgnoreCase(data.currency()) ? Moeda.BRL : Moeda.valueOf(data.currency());
        OffsetDateTime dataHora = Instant.parse(data.regularMarketTime()).atOffset(ZoneOffset.UTC);
        return new CotacaoExterna(data.longName(), moeda, preco, dataHora, PROVEDOR);
    }
}