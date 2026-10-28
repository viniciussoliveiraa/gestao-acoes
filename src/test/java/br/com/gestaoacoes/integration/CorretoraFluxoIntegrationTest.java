package br.com.gestaoacoes.integration;

import br.com.gestaoacoes.model.Papel;
import br.com.gestaoacoes.model.Usuario;
import br.com.gestaoacoes.repository.CorretoraRepository;
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
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class CorretoraFluxoIntegrationTest {

    static final MockWebServer brasilApiServer = new MockWebServer();
    static final MockWebServer viaCepServer = new MockWebServer();

    static {
        try {
            brasilApiServer.start(0);
            viaCepServer.start(0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.integrations.brasil-api.base-url", () -> "http://localhost:" + brasilApiServer.getPort());
        registry.add("app.integrations.via-cep.base-url", () -> "http://localhost:" + viaCepServer.getPort());
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CorretoraRepository repository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterAll
    static void stop() throws IOException {
        brasilApiServer.shutdown();
        viaCepServer.shutdown();
    }

    // Aula 14 (RBAC): cadastrar corretora agora exige papel ADMIN — cria um usuário ADMIN
    // diretamente (bypassando o autocadastro público, que sempre cria USER) e faz login real
    // para obter um token com a authority ROLE_ADMIN, replicando o que o Angular faria.
    private String tokenAdmin() throws Exception {
        String email = "admin.corretoras@exemplo.com";
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
    void fluxoCompletoDeCadastroPersisteEConsultaCorretora() throws Exception {
        String token = tokenAdmin();

        brasilApiServer.enqueue(new MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "razao_social": "Razao LTDA",
                          "nome_fantasia": "Fantasia",
                          "descricao_situacao_cadastral": "ATIVA",
                          "email": "contato@empresa.com",
                          "ddd_telefone_1": "1130000000"
                        }
                        """));
        brasilApiServer.enqueue(new MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"cnpj\":\"11222333000181\",\"status\":\"EM FUNCIONAMENTO NORMAL\"}"));
        viaCepServer.enqueue(new MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "logradouro": "Avenida Paulista",
                          "bairro": "Bela Vista",
                          "localidade": "São Paulo",
                          "uf": "SP"
                        }
                        """));

        mockMvc.perform(post("/corretoras")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"cnpj\":\"11.222.333/0001-81\",\"cep\":\"01310-100\",\"numero\":\"1000\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cnpj").value("11222333000181"))
                .andExpect(jsonPath("$.razaoSocial").value("Razao LTDA"));

        assertThat(repository.findByCnpj("11222333000181")).isPresent();

        Long id = repository.findByCnpj("11222333000181").orElseThrow().getId();
        mockMvc.perform(get("/corretoras/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uf").value("SP"));
    }

    @Test
    void cadastroSemTokenRetorna401() throws Exception {
        mockMvc.perform(post("/corretoras")
                        .contentType("application/json")
                        .content("{\"cnpj\":\"11.222.333/0001-81\",\"cep\":\"01310-100\",\"numero\":\"1000\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cadastroComUsuarioSemPapelAdminRetorna403() throws Exception {
        String email = "user.corretoras@exemplo.com";
        usuarioRepository.save(new Usuario("User", email, passwordEncoder.encode("senhaUser123"),
                Papel.USER, true, OffsetDateTime.now()));
        String respostaLogin = mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"" + email + "\",\"senha\":\"senhaUser123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(respostaLogin).get("token").asText();

        mockMvc.perform(post("/corretoras")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"cnpj\":\"11.222.333/0001-81\",\"cep\":\"01310-100\",\"numero\":\"1000\"}"))
                .andExpect(status().isForbidden());
    }
}