package br.com.gestaoacoes.integration;

import br.com.gestaoacoes.model.Papel;
import br.com.gestaoacoes.model.Usuario;
import br.com.gestaoacoes.repository.UsuarioRepository;
import br.com.gestaoacoes.security.JwtProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Aula 13, seção 13 — equivalente ao {@code AuthResourceIntegrationTest} do material: contexto
 * Spring completo (H2 + Liquibase + cadeia de segurança real), sem nenhum mock. Cobre também os
 * experimentos obrigatórios da seção 12.4 (token adulterado e token expirado devolvendo 401) por
 * HTTP, e não só no decodificador.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class AuthFluxoIntegrationTest {

    private static final String EMAIL_USER = "aluno.auth@exemplo.com";
    private static final String SENHA_USER = "senhaAluno123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private JwtProperties jwtProperties;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Long idUsuario;

    @BeforeEach
    void criarUsuario() {
        idUsuario = usuarioRepository.save(new Usuario("Aluno", EMAIL_USER, passwordEncoder.encode(SENHA_USER),
                Papel.USER, true, OffsetDateTime.now())).getId();
    }

    private String login(String email, String senha) throws Exception {
        String resposta = mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(resposta).get("token").asText();
    }

    @Test
    void loginComCredenciaisValidasEmiteJwtAssinado() throws Exception {
        String token = login(EMAIL_USER, SENHA_USER);

        // header.payload.assinatura
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void loginComSenhaIncorretaRetorna401() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"" + EMAIL_USER + "\",\"senha\":\"senhaErrada999\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void recursoProtegidoSemTokenRetorna401() throws Exception {
        mockMvc.perform(get("/carteira/posicoes")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void recursoProtegidoComTokenEmitidoRetorna200() throws Exception {
        String token = login(EMAIL_USER, SENHA_USER);

        mockMvc.perform(get("/carteira/posicoes").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void tokenAdulteradoRetorna401() throws Exception {
        String token = login(EMAIL_USER, SENHA_USER);
        // Altera um caractere do meio da assinatura (o último caractere de um base64url pode
        // carregar bits de preenchimento ignorados, então não é um bom ponto para adulterar).
        int posicao = token.lastIndexOf('.') + 5;
        char trocado = token.charAt(posicao) == 'A' ? 'B' : 'A';
        String adulterado = token.substring(0, posicao) + trocado + token.substring(posicao + 1);

        mockMvc.perform(get("/carteira/posicoes").header("Authorization", "Bearer " + adulterado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenExpiradoRetorna401() throws Exception {
        // Mesmo segredo e emissor do servidor (assinatura válida), mas com exp no passado —
        // equivale ao experimento JWT_EXPIRATION=10s da aula, sem esperar o relógio.
        Instant emitidoEm = Instant.now().minusSeconds(3600);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .issuedAt(emitidoEm)
                .expiresAt(emitidoEm.plusSeconds(60))
                .subject(String.valueOf(idUsuario))
                .claim("email", EMAIL_USER)
                .claim("authorities", List.of("ROLE_USER"))
                .build();
        String expirado = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).type("JWT").build(), claims)).getTokenValue();

        mockMvc.perform(get("/carteira/posicoes").header("Authorization", "Bearer " + expirado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void escritaComPapelUserRetorna403() throws Exception {
        String token = login(EMAIL_USER, SENHA_USER);

        mockMvc.perform(post("/corretoras")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        // Corpo válido: o Spring valida @Valid antes de o @PreAuthorize ser avaliado,
                        // então um corpo inválido daria 400 e mascararia a checagem de papel.
                        .content("{\"cnpj\":\"11.222.333/0001-81\",\"cep\":\"01310-100\",\"numero\":\"1000\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void meDevolveOUsuarioDoTokenSemExporSenha() throws Exception {
        String token = login(EMAIL_USER, SENHA_USER);

        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(idUsuario))
                .andExpect(jsonPath("$.email").value(EMAIL_USER))
                .andExpect(jsonPath("$.papel").value("USER"))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.senhaHash").doesNotExist());
    }
}
