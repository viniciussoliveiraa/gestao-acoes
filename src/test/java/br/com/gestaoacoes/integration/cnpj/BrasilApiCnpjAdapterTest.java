package br.com.gestaoacoes.integration.cnpj;

import br.com.gestaoacoes.exception.CnpjNaoEncontradoException;
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
class BrasilApiCnpjAdapterTest {

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
    private BrasilApiCnpjAdapter adapter;

    @AfterAll
    static void stop() throws IOException {
        server.shutdown();
    }

    @Test
    void consultaComSucessoMapeiaCamposEssenciais() {
        server.enqueue(new MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "razao_social": "Razao LTDA",
                          "nome_fantasia": "Fantasia",
                          "descricao_situacao_cadastral": "ATIVA",
                          "email": "contato@empresa.com",
                          "ddd_telefone_1": "1130000000",
                          "cep": "01310100",
                          "logradouro": "Av. Paulista",
                          "numero": "1000",
                          "complemento": "",
                          "bairro": "Bela Vista",
                          "municipio": "Sao Paulo",
                          "uf": "SP"
                        }
                        """));

        DadosCnpj dados = adapter.consultar("11222333000181");

        assertThat(dados.razaoSocial()).isEqualTo("Razao LTDA");
        assertThat(dados.situacaoCadastral()).isEqualTo("ATIVA");
        assertThat(dados.uf()).isEqualTo("SP");
    }

    @Test
    void cnpjNaoEncontradoLancaExcecaoEspecifica() {
        server.enqueue(new MockResponse().setResponseCode(404)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"message\":\"CNPJ não encontrado\"}"));

        assertThatThrownBy(() -> adapter.consultar("99999999000191"))
                .isInstanceOf(CnpjNaoEncontradoException.class);
    }

    @Test
    void falhaDeInfraestruturaLancaExcecaoDeIntegracao() {
        server.enqueue(new MockResponse().setResponseCode(500));

        assertThatThrownBy(() -> adapter.consultar("11222333000181"))
                .isInstanceOf(IntegracaoExternaIndisponivelException.class);
    }
}