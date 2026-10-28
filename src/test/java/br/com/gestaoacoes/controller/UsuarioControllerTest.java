package br.com.gestaoacoes.controller;

import br.com.gestaoacoes.config.SecurityConfig;
import br.com.gestaoacoes.exception.RecursoNaoEncontradoException;
import br.com.gestaoacoes.mapper.UsuarioMapper;
import br.com.gestaoacoes.model.Papel;
import br.com.gestaoacoes.model.Usuario;
import br.com.gestaoacoes.security.JwtPrincipalConverter;
import br.com.gestaoacoes.security.ProblemDetailAccessDeniedHandler;
import br.com.gestaoacoes.security.ProblemDetailAuthEntryPoint;
import br.com.gestaoacoes.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Toda a classe exige ADMIN (@PreAuthorize em UsuarioController); a stack de segurança real
// entra no contexto (mesma abordagem de CarteiraControllerTest) para exercitar 401/403 de verdade.
@WebMvcTest(UsuarioController.class)
@Import({UsuarioMapper.class, SecurityConfig.class, JwtPrincipalConverter.class,
        ProblemDetailAuthEntryPoint.class, ProblemDetailAccessDeniedHandler.class})
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioService service;

    private static UsernamePasswordAuthenticationToken comoAdmin() {
        return new UsernamePasswordAuthenticationToken(1L, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private static UsernamePasswordAuthenticationToken comoUser() {
        return new UsernamePasswordAuthenticationToken(2L, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    private Usuario usuario() {
        return new Usuario("Ana", "ana@exemplo.com", "hash", Papel.USER, true, OffsetDateTime.now());
    }

    @Test
    void listarComAdminRetorna200() throws Exception {
        when(service.listar(any())).thenReturn(new PageImpl<>(List.of(usuario())));

        mockMvc.perform(get("/usuarios").with(authentication(comoAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("ana@exemplo.com"));
    }

    @Test
    void buscarPorIdInexistenteRetorna404() throws Exception {
        when(service.buscarPorId(anyLong())).thenThrow(new RecursoNaoEncontradoException("Usuário não encontrado"));

        mockMvc.perform(get("/usuarios/99").with(authentication(comoAdmin())))
                .andExpect(status().isNotFound());
    }

    @Test
    void criarComAdminRetorna201() throws Exception {
        when(service.criar(any())).thenReturn(usuario());

        mockMvc.perform(post("/usuarios")
                        .with(authentication(comoAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Ana\",\"email\":\"ana@exemplo.com\",\"senha\":\"senhaSegura123\",\"papel\":\"USER\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("ana@exemplo.com"))
                .andExpect(jsonPath("$.senha").doesNotExist());
    }

    @Test
    void atualizarComAdminRetorna200() throws Exception {
        when(service.atualizar(anyLong(), any())).thenReturn(usuario());

        mockMvc.perform(put("/usuarios/1")
                        .with(authentication(comoAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Ana\",\"email\":\"ana@exemplo.com\",\"papel\":\"ADMIN\",\"ativo\":false}"))
                .andExpect(status().isOk());
    }

    @Test
    void alterarSenhaComAdminRetorna204() throws Exception {
        mockMvc.perform(put("/usuarios/1/senha")
                        .with(authentication(comoAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senha\":\"novaSenhaForte123\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void acessoSemTokenRetorna401() throws Exception {
        mockMvc.perform(get("/usuarios")).andExpect(status().isUnauthorized());
    }

    @Test
    void acessoComUsuarioComumRetorna403() throws Exception {
        mockMvc.perform(get("/usuarios").with(authentication(comoUser())))
                .andExpect(status().isForbidden());
    }
}
