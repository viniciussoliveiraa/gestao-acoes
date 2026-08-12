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
class TwelveDataAdapterTest {

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
    private TwelveDataAdapter adapter;

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
                          "symbol": "AAPL",
                          "name": "Apple Inc.",
                          "currency": "USD",
                          "close": "200.99",
                          "timestamp": 1755000000
                        }
                        """));

        CotacaoExterna cotacao = adapter.obterCotacao("AAPL");

        assertThat(cotacao.nomeEmpresa()).isEqualTo("Apple Inc.");
        assertThat(cotacao.moeda()).isEqualTo(Moeda.USD);
        assertThat(cotacao.preco()).isEqualByComparingTo(new BigDecimal("200.9900"));
    }

    @Test
    void chaveInvalidaLancaExcecaoDeIntegracao() {
        server.enqueue(new MockResponse().setResponseCode(401)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"code\":401,\"message\":\"apikey inválida\",\"status\":\"error\"}"));

        assertThatThrownBy(() -> adapter.obterCotacao("AAPL"))
                .isInstanceOf(IntegracaoExternaIndisponivelException.class);
    }

    @Test
    void simboloNaoEncontradoLancaTickerNaoEncontrado() {
        server.enqueue(new MockResponse().setResponseCode(400)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"code\":400,\"message\":\"**symbol** not found\",\"status\":\"error\"}"));

        assertThatThrownBy(() -> adapter.obterCotacao("ZZZZINVALID"))
                .isInstanceOf(TickerNaoEncontradoException.class);
    }
}