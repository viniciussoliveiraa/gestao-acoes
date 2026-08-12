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
class BrapiAdapterTest {

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
        registry.add("app.integrations.brapi.base-url", () -> "http://localhost:" + server.getPort());
    }

    @Autowired
    private BrapiAdapter adapter;

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
                          "results": [
                            {
                              "symbol": "PETR4",
                              "data": {
                                "longName": "Petroleo Brasileiro SA Pfd",
                                "currency": "BRL",
                                "regularMarketPrice": 41.62,
                                "regularMarketTime": "2026-08-12T16:32:30.000Z"
                              }
                            }
                          ]
                        }
                        """));

        CotacaoExterna cotacao = adapter.obterCotacao("PETR4");

        assertThat(cotacao.nomeEmpresa()).isEqualTo("Petroleo Brasileiro SA Pfd");
        assertThat(cotacao.moeda()).isEqualTo(Moeda.BRL);
        assertThat(cotacao.preco()).isEqualByComparingTo(new BigDecimal("41.6200"));
    }

    @Test
    void resultadoVazioLancaTickerNaoEncontrado() {
        server.enqueue(new MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"results\": []}"));

        assertThatThrownBy(() -> adapter.obterCotacao("ZZZZ9"))
                .isInstanceOf(TickerNaoEncontradoException.class);
    }

    @Test
    void tokenAusenteLancaExcecaoDeIntegracao() {
        server.enqueue(new MockResponse().setResponseCode(401)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"error\":true,\"message\":\"Token de autenticação não fornecido\",\"code\":\"MISSING_TOKEN\"}"));

        assertThatThrownBy(() -> adapter.obterCotacao("XPTO3"))
                .isInstanceOf(IntegracaoExternaIndisponivelException.class);
    }
}