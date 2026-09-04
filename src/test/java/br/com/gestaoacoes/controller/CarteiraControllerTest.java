package br.com.gestaoacoes.controller;

import br.com.gestaoacoes.config.SecurityConfig;
import br.com.gestaoacoes.dto.PosicaoResponse;
import br.com.gestaoacoes.exception.RecursoNaoEncontradoException;
import br.com.gestaoacoes.exception.SaldoInsuficienteException;
import br.com.gestaoacoes.mapper.LancamentoMapper;
import br.com.gestaoacoes.model.Acao;
import br.com.gestaoacoes.model.Corretora;
import br.com.gestaoacoes.model.Lancamento;
import br.com.gestaoacoes.model.Mercado;
import br.com.gestaoacoes.model.Moeda;
import br.com.gestaoacoes.model.TipoLancamento;
import br.com.gestaoacoes.security.JwtAuthenticationFilter;
import br.com.gestaoacoes.security.JwtService;
import br.com.gestaoacoes.security.ProblemDetailAccessDeniedHandler;
import br.com.gestaoacoes.security.ProblemDetailAuthEntryPoint;
import br.com.gestaoacoes.service.CarteiraService;
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

// Diferente de AcaoControllerTest/CorretoraControllerTest: /carteira exige autenticação, então
// aqui a stack de segurança real (SecurityConfig + filtro JWT + handlers) entra no contexto para
// exercitar de fato o 401 sem token e a autenticação simulada via SecurityMockMvcRequestPostProcessors.
@WebMvcTest(CarteiraController.class)
@Import({LancamentoMapper.class, SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class,
        ProblemDetailAuthEntryPoint.class, ProblemDetailAccessDeniedHandler.class})
class CarteiraControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CarteiraService service;

    private static UsernamePasswordAuthenticationToken autenticacaoDoUsuario(Long usuarioId) {
        return new UsernamePasswordAuthenticationToken(usuarioId, null, List.of());
    }

    @Test
    void registrarLancamentoSemTokenRetorna401() throws Exception {
        mockMvc.perform(post("/carteira/lancamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"acaoId\":1,\"corretoraId\":1,\"quantidade\":100,\"precoUnitario\":32.5,\"dataOperacao\":\"2026-08-10\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listarPosicoesSemTokenRetorna401() throws Exception {
        mockMvc.perform(get("/carteira/posicoes")).andExpect(status().isUnauthorized());
    }

    @Test
    void registrarLancamentoAutenticadoComSucessoRetorna201() throws Exception {
        when(service.registrarLancamento(any(), any())).thenReturn(mockLancamento());

        mockMvc.perform(post("/carteira/lancamentos")
                        .with(authentication(autenticacaoDoUsuario(42L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"acaoId\":1,\"corretoraId\":1,\"quantidade\":100,\"precoUnitario\":32.5,\"dataOperacao\":\"2026-08-10\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tickerAcao").value("PETR4"));
    }

    @Test
    void registrarLancamentoComAcaoInexistenteRetorna404() throws Exception {
        when(service.registrarLancamento(any(), any())).thenThrow(new RecursoNaoEncontradoException("não encontrada"));

        mockMvc.perform(post("/carteira/lancamentos")
                        .with(authentication(autenticacaoDoUsuario(42L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"acaoId\":99,\"corretoraId\":1,\"quantidade\":100,\"precoUnitario\":32.5,\"dataOperacao\":\"2026-08-10\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void registrarLancamentoComQuantidadeInvalidaRetorna400() throws Exception {
        mockMvc.perform(post("/carteira/lancamentos")
                        .with(authentication(autenticacaoDoUsuario(42L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"acaoId\":1,\"corretoraId\":1,\"quantidade\":0,\"precoUnitario\":32.5,\"dataOperacao\":\"2026-08-10\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registrarVendaAutenticadaComSucessoRetorna201() throws Exception {
        when(service.registrarLancamento(any(), any())).thenReturn(mockLancamento(TipoLancamento.VENDA));

        mockMvc.perform(post("/carteira/lancamentos")
                        .with(authentication(autenticacaoDoUsuario(42L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"acaoId\":1,\"corretoraId\":1,\"tipo\":\"VENDA\",\"quantidade\":40,\"precoUnitario\":35.0,\"dataOperacao\":\"2026-08-20\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("VENDA"));
    }

    @Test
    void registrarVendaAcimaDoSaldoRetorna422() throws Exception {
        when(service.registrarLancamento(any(), any())).thenThrow(new SaldoInsuficienteException("saldo insuficiente"));

        mockMvc.perform(post("/carteira/lancamentos")
                        .with(authentication(autenticacaoDoUsuario(42L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"acaoId\":1,\"corretoraId\":1,\"tipo\":\"VENDA\",\"quantidade\":150,\"precoUnitario\":35.0,\"dataOperacao\":\"2026-08-20\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void listarPosicoesAutenticadoRetorna200() throws Exception {
        when(service.listarPosicoes(anyLong())).thenReturn(List.of(
                new PosicaoResponse(1L, "PETR4", "Petrobras", new BigDecimal("100"), new BigDecimal("32.5000"),
                        new BigDecimal("3250.0000"), new BigDecimal("3500.0000"), new BigDecimal("7.6900"),
                        new BigDecimal("0.0000"))));

        mockMvc.perform(get("/carteira/posicoes").with(authentication(autenticacaoDoUsuario(42L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ticker").value("PETR4"));
    }

    @Test
    void listarLancamentosAutenticadoRetorna200() throws Exception {
        when(service.listarLancamentos(anyLong(), any())).thenReturn(new PageImpl<>(List.of(mockLancamento(TipoLancamento.COMPRA))));

        mockMvc.perform(get("/carteira/lancamentos").with(authentication(autenticacaoDoUsuario(42L))))
                .andExpect(status().isOk());
    }

    private Lancamento mockLancamento() {
        return mockLancamento(TipoLancamento.COMPRA);
    }

    private Lancamento mockLancamento(TipoLancamento tipo) {
        Acao acao = new Acao("PETR4", "Petrobras", Mercado.BRASIL, Moeda.BRL, new BigDecimal("35.0000"),
                OffsetDateTime.now(), "teste", OffsetDateTime.now());
        Corretora corretora = new Corretora("11222333000181", "Razao LTDA", "Fantasia", null, null,
                "01310100", "Av. Paulista", "1000", null, "Bela Vista", "Sao Paulo", "SP",
                "ATIVA", true, OffsetDateTime.now());
        return new Lancamento(42L, acao, corretora, tipo, new BigDecimal("100"), new BigDecimal("32.50"),
                java.time.LocalDate.now(), OffsetDateTime.now());
    }
}