package br.com.gestaoacoes.integration.instituicao;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class BrasilApiCvmAdapterTest {

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
        registry.add("app.integrations.brasil-api.base-url", () -> "http://localhost:" + server.getPort());
    }

    @Autowired
    private BrasilApiCvmAdapter adapter;

    @AfterAll
    static void stop() throws IOException {
        server.shutdown();
    }

    @Test
    void instituicaoEmFuncionamentoNormalEhValidada() {
        server.enqueue(new MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"cnpj\":\"44527444000155\",\"status\":\"EM FUNCIONAMENTO NORMAL\"}"));

        assertThat(adapter.validar("44527444000155")).isTrue();
    }

    @Test
    void instituicaoCanceladaNaoEhValidada() {
        server.enqueue(new MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"cnpj\":\"76621457000185\",\"status\":\"CANCELADA\"}"));

        assertThat(adapter.validar("76621457000185")).isFalse();
    }

    @Test
    void cnpjNaoEncontradoNaCvmNaoEhValidado() {
        server.enqueue(new MockResponse().setResponseCode(404)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"name\":\"EXCHANGE_NOT_FOUND\"}"));

        assertThat(adapter.validar("11222333000181")).isFalse();
    }

    @Test
    void falhaDeInfraestruturaLancaExcecaoDeIntegracao() {
        server.enqueue(new MockResponse().setResponseCode(500));

        assertThatThrownBy(() -> adapter.validar("44527444000155"))
                .isInstanceOf(IntegracaoExternaIndisponivelException.class);
    }
}