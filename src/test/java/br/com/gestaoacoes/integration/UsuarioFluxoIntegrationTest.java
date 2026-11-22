package br.com.gestaoacoes.integration;

import br.com.gestaoacoes.model.Papel;
import br.com.gestaoacoes.model.Usuario;
import br.com.gestaoacoes.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Aula 14, seção 11 — equivalente ao {@code UsuarioResourceIntegrationTest} do material: contexto
 * completo (H2 + Liquibase + segurança + serviço + repositório reais), verificando no banco o que
 * o teste de controller com serviço simulado ({@code UsuarioControllerTest}) não consegue ver.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class UsuarioFluxoIntegrationTest {

    private static final String EMAIL_ADMIN = "admin.usuarios@exemplo.com";
    private static final String SENHA_ADMIN = "senhaAdmin123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Long idAdmin;
    private String tokenAdmin;

    @BeforeEach
    void criarAdministrador() throws Exception {
        idAdmin = usuarioRepository.save(new Usuario("Admin", EMAIL_ADMIN, passwordEncoder.encode(SENHA_ADMIN),
                Papel.ADMIN, true, OffsetDateTime.now())).getId();
        tokenAdmin = token(EMAIL_ADMIN, SENHA_ADMIN);
    }

    private ResultActions login(String email, String senha) throws Exception {
        return mockMvc.perform(post("/auth/login")
                .contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}"));
    }

    private String token(String email, String senha) throws Exception {
        String resposta = login(email, senha).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(resposta).get("token").asText();
    }

    private Long criarUsuario(String email, String senha, Papel papel) throws Exception {
        String resposta = mockMvc.perform(post("/usuarios")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType("application/json")
                        .content("{\"nome\":\"Fulano\",\"email\":\"" + email + "\",\"senha\":\"" + senha
                                + "\",\"papel\":\"" + papel + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(resposta).get("id").asLong();
    }

    @Test
    void administradorCriaUsuarioComHashBcryptSemExporSenhaNaResposta() throws Exception {
        mockMvc.perform(post("/usuarios")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType("application/json")
                        .content("{\"nome\":\"Ana\",\"email\":\"ana@exemplo.com\",\"senha\":\"senhaDaAna123\",\"papel\":\"USER\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("ana@exemplo.com"))
                .andExpect(jsonPath("$.papel").value("USER"))
                .andExpect(jsonPath("$.ativo").value(true))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.senhaHash").doesNotExist());

        String hashGravado = usuarioRepository.findByEmail("ana@exemplo.com").orElseThrow().getSenhaHash();
        assertThat(hashGravado).isNotEqualTo("senhaDaAna123").startsWith("$2");
        assertThat(passwordEncoder.matches("senhaDaAna123", hashGravado)).isTrue();
    }

    @Test
    void listagemAdministrativaNaoExpoeSenhaNemHash() throws Exception {
        criarUsuario("bia@exemplo.com", "senhaDaBia123", Papel.USER);

        String corpo = mockMvc.perform(get("/usuarios").header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(corpo).contains("bia@exemplo.com", EMAIL_ADMIN);
        assertThat(corpo).doesNotContain("senha").doesNotContain("$2");
    }

    @Test
    void alteracaoDePapelESituacaoEhPersistidaEDesativadoNaoAutentica() throws Exception {
        Long id = criarUsuario("caio@exemplo.com", "senhaDoCaio123", Papel.USER);

        mockMvc.perform(put("/usuarios/" + id)
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType("application/json")
                        .content("{\"nome\":\"Caio Lima\",\"email\":\"caio@exemplo.com\",\"papel\":\"ADMIN\",\"ativo\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Caio Lima"))
                .andExpect(jsonPath("$.papel").value("ADMIN"))
                .andExpect(jsonPath("$.ativo").value(false));

        Usuario gravado = usuarioRepository.findById(id).orElseThrow();
        assertThat(gravado.getNome()).isEqualTo("Caio Lima");
        assertThat(gravado.getPapel()).isEqualTo(Papel.ADMIN);
        assertThat(gravado.getAtivo()).isFalse();

        login("caio@exemplo.com", "senhaDoCaio123").andExpect(status().isUnauthorized());
    }

    @Test
    void trocaDeSenhaInvalidaASenhaAntigaEAceitaANova() throws Exception {
        Long id = criarUsuario("davi@exemplo.com", "senhaAntiga123", Papel.USER);

        mockMvc.perform(put("/usuarios/" + id + "/senha")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType("application/json")
                        .content("{\"senha\":\"senhaNova456\"}"))
                .andExpect(status().isNoContent());

        login("davi@exemplo.com", "senhaAntiga123").andExpect(status().isUnauthorized());
        login("davi@exemplo.com", "senhaNova456").andExpect(status().isOk());
    }

    @Test
    void naoPermiteRebaixarOUltimoAdministradorAtivo() throws Exception {
        mockMvc.perform(put("/usuarios/" + idAdmin)
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType("application/json")
                        .content("{\"nome\":\"Admin\",\"email\":\"" + EMAIL_ADMIN + "\",\"papel\":\"USER\",\"ativo\":true}"))
                .andExpect(status().isUnprocessableEntity());

        assertThat(usuarioRepository.findById(idAdmin).orElseThrow().getPapel()).isEqualTo(Papel.ADMIN);
    }

    @Test
    void usuarioComumComTokenRealNaoGerenciaUsuarios() throws Exception {
        criarUsuario("eva@exemplo.com", "senhaDaEva123", Papel.USER);
        String tokenUser = token("eva@exemplo.com", "senhaDaEva123");

        mockMvc.perform(get("/usuarios").header("Authorization", "Bearer " + tokenUser))
                .andExpect(status().isForbidden());
    }

    @Test
    void semTokenRetorna401() throws Exception {
        mockMvc.perform(get("/usuarios")).andExpect(status().isUnauthorized());
    }

    // Exemplo da própria aula (seção 11): @WithMockUser isola a regra de autorização, sem login.
    // setupBefore = TEST_EXECUTION: o login feito no @BeforeEach limpa o SecurityContext ao fim
    // da requisição, então o usuário simulado precisa ser montado depois dele.
    @Test
    @WithMockUser(username = "aluno", roles = "USER", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void userNaoDeveGerenciarUsuarios() throws Exception {
        mockMvc.perform(get("/usuarios")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "professor", roles = "ADMIN", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void adminSimuladoListaUsuarios() throws Exception {
        mockMvc.perform(get("/usuarios")).andExpect(status().isOk());
    }
}
