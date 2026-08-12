package br.com.gestaoacoes.controller;

import br.com.gestaoacoes.exception.CnpjInvalidoException;
import br.com.gestaoacoes.exception.CorretoraDuplicadaException;
import br.com.gestaoacoes.exception.InstituicaoNaoValidadaException;
import br.com.gestaoacoes.exception.RecursoNaoEncontradoException;
import br.com.gestaoacoes.mapper.CorretoraMapper;
import br.com.gestaoacoes.model.Corretora;
import br.com.gestaoacoes.service.CorretoraService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CorretoraController.class)
@Import(CorretoraMapper.class)
class CorretoraControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CorretoraService service;

    @Test
    void registrarComSucessoRetorna201() throws Exception {
        when(service.registrar(any())).thenReturn(mockCorretora());

        mockMvc.perform(post("/corretoras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cnpj\":\"11222333000181\",\"cep\":\"01310100\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cnpj").value("11222333000181"));
    }

    @Test
    void payloadSemCnpjRetorna400() throws Exception {
        mockMvc.perform(post("/corretoras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cnpjInvalidoRetorna400() throws Exception {
        when(service.registrar(any())).thenThrow(new CnpjInvalidoException("CNPJ inválido"));

        mockMvc.perform(post("/corretoras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cnpj\":\"111\",\"cep\":\"01310100\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void instituicaoNaoValidadaRetorna422() throws Exception {
        when(service.registrar(any())).thenThrow(new InstituicaoNaoValidadaException("não validada"));

        mockMvc.perform(post("/corretoras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cnpj\":\"11222333000181\",\"cep\":\"01310100\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void cnpjDuplicadoRetorna409() throws Exception {
        when(service.registrar(any())).thenThrow(new CorretoraDuplicadaException("duplicado"));

        mockMvc.perform(post("/corretoras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cnpj\":\"11222333000181\",\"cep\":\"01310100\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void buscarPorIdInexistenteRetorna404() throws Exception {
        when(service.buscarPorId(anyLong())).thenThrow(new RecursoNaoEncontradoException("não encontrada"));

        mockMvc.perform(get("/corretoras/99")).andExpect(status().isNotFound());
    }

    @Test
    void buscarPorCnpjInexistenteRetorna404() throws Exception {
        when(service.buscarPorCnpj(anyString())).thenThrow(new RecursoNaoEncontradoException("não encontrada"));

        mockMvc.perform(get("/corretoras/cnpj/11222333000181")).andExpect(status().isNotFound());
    }

    @Test
    void listarRetorna200ComPaginacao() throws Exception {
        when(service.listar(any())).thenReturn(new PageImpl<>(List.of(mockCorretora())));

        mockMvc.perform(get("/corretoras?page=0&size=20")).andExpect(status().isOk());
    }

    private Corretora mockCorretora() {
        return new Corretora("11222333000181", "Razao LTDA", "Fantasia", "contato@empresa.com", "1130000000",
                "01310100", "Av. Paulista", "1000", null, "Bela Vista", "Sao Paulo", "SP", "ATIVA", true,
                OffsetDateTime.now());
    }
}