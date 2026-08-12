package br.com.gestaoacoes.integration.cotacao;

import br.com.gestaoacoes.exception.IntegracaoExternaIndisponivelException;
import br.com.gestaoacoes.exception.TickerNaoEncontradoException;
import br.com.gestaoacoes.model.Moeda;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class AlphaVantageAdapterTest {

    static final MockWebServer server = new MockWebServer();

    static {
        try {
            server.start(0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.integrations.alpha-vantage.base-url", () -> "http://localhost:" + server.getPort());
    }

    @Autowired
    private AlphaVantageAdapter adapter;

    @AfterAll
    static void stop() throws IOException {
        server.shutdown();
    }

    @Test
    void consultaComSucessoMapeiaCotacao() {
        server.enqueue(new MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "Global Quote": {
                            "01. symbol": "IBM",
                            "05. price": "238.4200",
                            "07. latest trading day": "2026-08-11"
                          }
                        }
                        """));

        CotacaoExterna cotacao = adapter.obterCotacao("IBM");

        assertThat(cotacao.moeda()).isEqualTo(Moeda.USD);
        assertThat(cotacao.preco()).isEqualByComparingTo(new BigDecimal("238.4200"));
    }

    @Test
    void globalQuoteVazioLancaTickerNaoEncontrado() {
        server.enqueue(new MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"Global Quote\": {}}"));

        assertThatThrownBy(() -> adapter.obterCotacao("ZZZZINVALID"))
                .isInstanceOf(TickerNaoEncontradoException.class);
    }

    @Test
    void limiteDeRequisicoesLancaExcecaoDeIntegracao() {
        server.enqueue(new MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"Note\": \"Thank you for using Alpha Vantage! Our standard API rate limit is...\"}"));

        assertThatThrownBy(() -> adapter.obterCotacao("IBM"))
                .isInstanceOf(IntegracaoExternaIndisponivelException.class);
    }
}