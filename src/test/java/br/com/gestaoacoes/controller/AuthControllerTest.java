package br.com.gestaoacoes.controller;

import br.com.gestaoacoes.config.SecurityConfig;
import br.com.gestaoacoes.exception.CredenciaisInvalidasException;
import br.com.gestaoacoes.exception.EmailJaCadastradoException;
import br.com.gestaoacoes.mapper.UsuarioMapper;
import br.com.gestaoacoes.model.Papel;
import br.com.gestaoacoes.model.Usuario;
import br.com.gestaoacoes.security.JwtPrincipalConverter;
import br.com.gestaoacoes.security.ProblemDetailAccessDeniedHandler;
import br.com.gestaoacoes.security.ProblemDetailAuthEntryPoint;
import br.com.gestaoacoes.service.AuthService;
import br.com.gestaoacoes.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// /auth/registrar e /auth/login são públicos; /auth/me exige autenticação. Diferente de
// AcaoControllerTest/CorretoraControllerTest: aqui a stack de segurança real entra no contexto
// (mesma abordagem de CarteiraControllerTest) porque SecurityMockMvcRequestPostProcessors.
// authentication(...) depende do SecurityContextHolderFilter real para popular o contexto —
// com addFilters = false ele nunca chegaria a rodar, e @AuthenticationPrincipal resolveria null.
@WebMvcTest(AuthController.class)
@Import({UsuarioMapper.class, SecurityConfig.class, JwtPrincipalConverter.class,
        ProblemDetailAuthEntryPoint.class, ProblemDetailAccessDeniedHandler.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService service;

    @MockitoBean
    private UsuarioService usuarioService;

    private static org.springframework.security.authentication.UsernamePasswordAuthenticationToken autenticacaoDoUsuario(Long usuarioId) {
        return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(usuarioId, null, List.of());
    }

    @Test
    void registrarComSucessoRetorna201() throws Exception {
        when(service.registrar(any())).thenReturn(new Usuario("Ana", "ana@exemplo.com", "hash", OffsetDateTime.now()));

        mockMvc.perform(post("/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Ana\",\"email\":\"ana@exemplo.com\",\"senha\":\"senhaSegura123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("ana@exemplo.com"))
                .andExpect(jsonPath("$.papel").value("USER"))
                .andExpect(jsonPath("$.senha").doesNotExist());
    }

    @Test
    void registrarComEmailDuplicadoRetorna409() throws Exception {
        when(service.registrar(any())).thenThrow(new EmailJaCadastradoException("já cadastrado"));

        mockMvc.perform(post("/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Ana\",\"email\":\"ana@exemplo.com\",\"senha\":\"senhaSegura123\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void registrarComSenhaCurtaRetorna400() throws Exception {
        mockMvc.perform(post("/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Ana\",\"email\":\"ana@exemplo.com\",\"senha\":\"123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginComCredenciaisValidasRetorna200ComToken() throws Exception {
        when(service.login(any())).thenReturn("token-jwt-fake");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@exemplo.com\",\"senha\":\"senhaSegura123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-jwt-fake"));
    }

    @Test
    void loginComCredenciaisInvalidasRetorna401() throws Exception {
        when(service.login(any())).thenThrow(new CredenciaisInvalidasException("Email ou senha inválidos"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@exemplo.com\",\"senha\":\"errada\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meRetornaUsuarioDoTokenAutenticado() throws Exception {
        Usuario usuario = new Usuario("Ana", "ana@exemplo.com", "hash", Papel.ADMIN, true, OffsetDateTime.now());
        when(usuarioService.buscarPorId(anyLong())).thenReturn(usuario);

        mockMvc.perform(get("/auth/me").with(authentication(autenticacaoDoUsuario(7L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ana@exemplo.com"))
                .andExpect(jsonPath("$.papel").value("ADMIN"));
    }
}
