package br.com.gestaoacoes.controller;

import br.com.gestaoacoes.exception.CredenciaisInvalidasException;
import br.com.gestaoacoes.exception.EmailJaCadastradoException;
import br.com.gestaoacoes.mapper.UsuarioMapper;
import br.com.gestaoacoes.model.Usuario;
import br.com.gestaoacoes.security.JwtService;
import br.com.gestaoacoes.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// /auth é público (SecurityConfig); ver nota equivalente em CorretoraControllerTest sobre
// addFilters = false e a importação de JwtService.
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({UsuarioMapper.class, JwtService.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService service;

    @Test
    void registrarComSucessoRetorna201() throws Exception {
        when(service.registrar(any())).thenReturn(new Usuario("Ana", "ana@exemplo.com", "hash", OffsetDateTime.now()));

        mockMvc.perform(post("/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Ana\",\"email\":\"ana@exemplo.com\",\"senha\":\"senhaSegura123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("ana@exemplo.com"))
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
}