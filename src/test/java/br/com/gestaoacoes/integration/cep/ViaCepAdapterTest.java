package br.com.gestaoacoes.integration.cep;

import br.com.gestaoacoes.exception.CepNaoEncontradoException;
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
class ViaCepAdapterTest {

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
        registry.add("app.integrations.via-cep.base-url", () -> "http://localhost:" + server.getPort());
    }

    @Autowired
    private ViaCepAdapter adapter;

    @AfterAll
    static void stop() throws IOException {
        server.shutdown();
    }

    @Test
    void consultaComSucessoMapeiaEndereco() {
        server.enqueue(new MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "cep": "01310-100",
                          "logradouro": "Avenida Paulista",
                          "bairro": "Bela Vista",
                          "localidade": "São Paulo",
                          "uf": "SP"
                        }
                        """));

        Endereco endereco = adapter.consultar("01310100");

        assertThat(endereco.logradouro()).isEqualTo("Avenida Paulista");
        assertThat(endereco.cidade()).isEqualTo("São Paulo");
        assertThat(endereco.uf()).isEqualTo("SP");
    }

    @Test
    void cepInexistenteLancaExcecaoEspecifica() {
        server.enqueue(new MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"erro\":\"true\"}"));

        assertThatThrownBy(() -> adapter.consultar("99999999"))
                .isInstanceOf(CepNaoEncontradoException.class);
    }

    @Test
    void falhaDeInfraestruturaLancaExcecaoDeIntegracao() {
        server.enqueue(new MockResponse().setResponseCode(500));

        assertThatThrownBy(() -> adapter.consultar("01310100"))
                .isInstanceOf(IntegracaoExternaIndisponivelException.class);
    }
}