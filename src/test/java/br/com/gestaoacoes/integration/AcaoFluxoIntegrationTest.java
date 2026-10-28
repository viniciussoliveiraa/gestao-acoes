package br.com.gestaoacoes.integration;

import br.com.gestaoacoes.model.Papel;
import br.com.gestaoacoes.model.Usuario;
import br.com.gestaoacoes.repository.AcaoRepository;
import br.com.gestaoacoes.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

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

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterAll
    static void stop() throws IOException {
        brapiServer.shutdown();
    }

    // Aula 14 (RBAC): cadastrar ação e atualizar cotação agora exigem papel ADMIN.
    private String tokenAdmin() throws Exception {
        String email = "admin.acoes@exemplo.com";
        usuarioRepository.save(new Usuario("Admin", email, passwordEncoder.encode("senhaAdmin123"),
                Papel.ADMIN, true, OffsetDateTime.now()));

        String respostaLogin = mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"" + email + "\",\"senha\":\"senhaAdmin123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(respostaLogin).get("token").asText();
    }

    @Test
    void fluxoCompletoDeCadastroEAtualizacaoDeCotacao() throws Exception {
        String token = tokenAdmin();

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
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"ticker\":\"petr4\",\"mercado\":\"BRASIL\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticker").value("PETR4"))
                .andExpect(jsonPath("$.cotacaoAtual").value(38.42));

        Long id = repository.findByTicker("PETR4").orElseThrow().getId();

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

        mockMvc.perform(put("/acoes/" + id + "/atualizar-cotacao").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cotacaoAtual").value(40.10));

        assertThat(repository.findById(id).orElseThrow().getCotacaoAtual())
                .isEqualByComparingTo(new BigDecimal("40.1000"));
    }

    @Test
    void cadastroSemTokenRetorna401() throws Exception {
        mockMvc.perform(post("/acoes")
                        .contentType("application/json")
                        .content("{\"ticker\":\"petr4\",\"mercado\":\"BRASIL\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cadastroComUsuarioSemPapelAdminRetorna403() throws Exception {
        String email = "user.acoes@exemplo.com";
        usuarioRepository.save(new Usuario("User", email, passwordEncoder.encode("senhaUser123"),
                Papel.USER, true, OffsetDateTime.now()));
        String respostaLogin = mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"" + email + "\",\"senha\":\"senhaUser123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(respostaLogin).get("token").asText();

        mockMvc.perform(post("/acoes")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"ticker\":\"petr4\",\"mercado\":\"BRASIL\"}"))
                .andExpect(status().isForbidden());
    }
}