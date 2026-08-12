package br.com.gestaoacoes.controller;

import br.com.gestaoacoes.exception.AcaoDuplicadaException;
import br.com.gestaoacoes.exception.IntegracaoExternaIndisponivelException;
import br.com.gestaoacoes.exception.RecursoNaoEncontradoException;
import br.com.gestaoacoes.exception.TickerAmbiguoException;
import br.com.gestaoacoes.exception.TickerNaoEncontradoException;
import br.com.gestaoacoes.mapper.AcaoMapper;
import br.com.gestaoacoes.model.Acao;
import br.com.gestaoacoes.model.Mercado;
import br.com.gestaoacoes.model.Moeda;
import br.com.gestaoacoes.service.AcaoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AcaoController.class)
@Import(AcaoMapper.class)
class AcaoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AcaoService service;

    @Test
    void registrarComSucessoRetorna201() throws Exception {
        when(service.registrar(any())).thenReturn(mockAcao());

        mockMvc.perform(post("/acoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticker\":\"PETR4\",\"mercado\":\"BRASIL\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticker").value("PETR4"));
    }

    @Test
    void payloadSemTickerRetorna400() throws Exception {
        mockMvc.perform(post("/acoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void tickerNaoEncontradoRetorna404() throws Exception {
        when(service.registrar(any())).thenThrow(new TickerNaoEncontradoException("não encontrado"));

        mockMvc.perform(post("/acoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticker\":\"ZZZZ9\",\"mercado\":\"BRASIL\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void acaoDuplicadaRetorna409() throws Exception {
        when(service.registrar(any())).thenThrow(new AcaoDuplicadaException("duplicado"));

        mockMvc.perform(post("/acoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticker\":\"PETR4\",\"mercado\":\"BRASIL\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void provedorIndisponivelRetorna502() throws Exception {
        when(service.registrar(any())).thenThrow(new IntegracaoExternaIndisponivelException("indisponível"));

        mockMvc.perform(post("/acoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticker\":\"PETR4\",\"mercado\":\"BRASIL\"}"))
                .andExpect(status().isBadGateway());
    }

    @Test
    void buscarPorTickerAmbiguoRetorna400() throws Exception {
        when(service.buscarPorTicker(anyString(), org.mockito.ArgumentMatchers.isNull()))
                .thenThrow(new TickerAmbiguoException("ambíguo"));

        mockMvc.perform(get("/acoes/ticker/IBM")).andExpect(status().isBadRequest());
    }

    @Test
    void buscarPorIdInexistenteRetorna404() throws Exception {
        when(service.buscarPorId(anyLong())).thenThrow(new RecursoNaoEncontradoException("não encontrada"));

        mockMvc.perform(get("/acoes/99")).andExpect(status().isNotFound());
    }

    @Test
    void atualizarCotacaoComSucessoRetorna200() throws Exception {
        when(service.atualizarCotacao(anyLong())).thenReturn(mockAcao());

        mockMvc.perform(put("/acoes/1/atualizar-cotacao")).andExpect(status().isOk());
    }

    @Test
    void listarRetorna200ComPaginacao() throws Exception {
        when(service.listar(any())).thenReturn(new PageImpl<>(List.of(mockAcao())));

        mockMvc.perform(get("/acoes?page=0&size=20")).andExpect(status().isOk());
    }

    private Acao mockAcao() {
        return new Acao("PETR4", "Petrobras", Mercado.BRASIL, Moeda.BRL, new BigDecimal("38.4200"),
                OffsetDateTime.now(), "brapi", OffsetDateTime.now());
    }
}