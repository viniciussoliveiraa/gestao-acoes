package br.com.gestaoacoes.integration;

import br.com.gestaoacoes.repository.AcaoRepository;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class AcaoFluxoIntegrationTest {

    static final MockWebServer brapiServer = new MockWebServer();

    static {
        try {
            brapiServer.start(0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.integrations.brapi.base-url", () -> "http://localhost:" + brapiServer.getPort());
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AcaoRepository repository;

    @AfterAll
    static void stop() throws IOException {
        brapiServer.shutdown();
    }

    @Test
    void fluxoCompletoDeCadastroEAtualizacaoDeCotacao() throws Exception {
        brapiServer.enqueue(new MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "results": [
                            {
                              "symbol": "PETR4",
                              "data": {
                                "longName": "Petroleo Brasileiro SA Pfd",
                                "currency": "BRL",
                                "regularMarketPrice": 38.42,
                                "regularMarketTime": "2026-08-12T16:32:30.000Z"
                              }
                            }
                          ]
                        }
                        """));

        mockMvc.perform(post("/acoes")
                        .contentType("application/json")
                        .content("{\"ticker\":\"petr4\",\"mercado\":\"BRASIL\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticker").value("PETR4"))
                .andExpect(jsonPath("$.cotacaoAtual").value(38.42));

        Long id = repository.findByTickerAndMercado("PETR4", br.com.gestaoacoes.model.Mercado.BRASIL)
                .orElseThrow().getId();

        brapiServer.enqueue(new MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "results": [
                            {
                              "symbol": "PETR4",
                              "data": {
                                "longName": "Petroleo Brasileiro SA Pfd",
                                "currency": "BRL",
                                "regularMarketPrice": 40.10,
                                "regularMarketTime": "2026-08-12T18:00:00.000Z"
                              }
                            }
                          ]
                        }
                        """));

        mockMvc.perform(put("/acoes/" + id + "/atualizar-cotacao"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cotacaoAtual").value(40.10));

        assertThat(repository.findById(id).orElseThrow().getCotacaoAtual())
                .isEqualByComparingTo(new BigDecimal("40.1000"));
    }
}