package br.com.gestaoacoes.controller;

import br.com.gestaoacoes.config.SecurityConfig;
import br.com.gestaoacoes.exception.RecursoNaoEncontradoException;
import br.com.gestaoacoes.mapper.ProventoMapper;
import br.com.gestaoacoes.model.Acao;
import br.com.gestaoacoes.model.Mercado;
import br.com.gestaoacoes.model.Moeda;
import br.com.gestaoacoes.model.Provento;
import br.com.gestaoacoes.model.TipoProvento;
import br.com.gestaoacoes.security.JwtAuthenticationFilter;
import br.com.gestaoacoes.security.JwtService;
import br.com.gestaoacoes.security.ProblemDetailAccessDeniedHandler;
import br.com.gestaoacoes.security.ProblemDetailAuthEntryPoint;
import br.com.gestaoacoes.service.ProventoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// /proventos exige autenticação: mesma abordagem de CarteiraControllerTest (stack de segurança
// real importada para exercitar 401 sem token e autenticação simulada via postprocessor).
@WebMvcTest(ProventoController.class)
@Import({ProventoMapper.class, SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class,
        ProblemDetailAuthEntryPoint.class, ProblemDetailAccessDeniedHandler.class})
class ProventoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProventoService service;

    private static UsernamePasswordAuthenticationToken autenticacaoDoUsuario(Long usuarioId) {
        return new UsernamePasswordAuthenticationToken(usuarioId, null, List.of());
    }

    @Test
    void registrarSemTokenRetorna401() throws Exception {
        mockMvc.perform(post("/proventos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"acaoId\":1,\"tipo\":\"DIVIDENDO\",\"valorTotal\":45.9,\"dataPagamento\":\"2026-07-15\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listarSemTokenRetorna401() throws Exception {
        mockMvc.perform(get("/proventos")).andExpect(status().isUnauthorized());
    }

    @Test
    void registrarAutenticadoComSucessoRetorna201() throws Exception {
        when(service.registrar(any(), any())).thenReturn(mockProvento());

        mockMvc.perform(post("/proventos")
                        .with(authentication(autenticacaoDoUsuario(42L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"acaoId\":1,\"tipo\":\"DIVIDENDO\",\"valorTotal\":45.9,\"dataPagamento\":\"2026-07-15\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tickerAcao").value("PETR4"));
    }

    @Test
    void registrarComAcaoInexistenteRetorna404() throws Exception {
        when(service.registrar(any(), any())).thenThrow(new RecursoNaoEncontradoException("não encontrada"));

        mockMvc.perform(post("/proventos")
                        .with(authentication(autenticacaoDoUsuario(42L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"acaoId\":99,\"tipo\":\"DIVIDENDO\",\"valorTotal\":45.9,\"dataPagamento\":\"2026-07-15\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void registrarComValorInvalidoRetorna400() throws Exception {
        mockMvc.perform(post("/proventos")
                        .with(authentication(autenticacaoDoUsuario(42L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"acaoId\":1,\"tipo\":\"DIVIDENDO\",\"valorTotal\":0,\"dataPagamento\":\"2026-07-15\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listarAutenticadoRetorna200() throws Exception {
        when(service.listar(any(), any())).thenReturn(new PageImpl<>(List.of(mockProvento())));

        mockMvc.perform(get("/proventos").with(authentication(autenticacaoDoUsuario(42L))))
                .andExpect(status().isOk());
    }

    private Provento mockProvento() {
        Acao acao = new Acao("PETR4", "Petrobras", Mercado.BRASIL, Moeda.BRL, new BigDecimal("35.0000"),
                OffsetDateTime.now(), "teste", OffsetDateTime.now());
        return new Provento(42L, acao, TipoProvento.DIVIDENDO, new BigDecimal("45.90"),
                LocalDate.of(2026, 7, 15), OffsetDateTime.now());
    }
}