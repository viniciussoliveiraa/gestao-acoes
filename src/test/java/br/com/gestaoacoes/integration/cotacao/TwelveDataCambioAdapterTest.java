package br.com.gestaoacoes.integration.cotacao;

import br.com.gestaoacoes.exception.IntegracaoExternaIndisponivelException;
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
class TwelveDataCambioAdapterTest {

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
        registry.add("app.integrations.twelve-data.base-url", () -> "http://localhost:" + server.getPort());
    }

    @Autowired
    private TwelveDataCambioAdapter adapter;

    @AfterAll
    static void stop() throws IOException {
        server.shutdown();
    }

    @Test
    void consultaComSucessoRetornaTaxaDeCambio() {
        server.enqueue(new MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "symbol": "USD/BRL",
                          "name": "USD/BRL",
                          "close": "5.4321",
                          "timestamp": 1755000000
                        }
                        """));

        BigDecimal taxa = adapter.obterCotacaoUsdParaBrl();

        assertThat(taxa).isEqualByComparingTo(new BigDecimal("5.4321"));
    }

    @Test
    void chaveInvalidaLancaExcecaoDeIntegracao() {
        server.enqueue(new MockResponse().setResponseCode(401)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"code\":401,\"message\":\"apikey inválida\",\"status\":\"error\"}"));

        assertThatThrownBy(() -> adapter.obterCotacaoUsdParaBrl())
                .isInstanceOf(IntegracaoExternaIndisponivelException.class);
    }
}
