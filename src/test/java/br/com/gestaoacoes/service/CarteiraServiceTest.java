package br.com.gestaoacoes.service;

import br.com.gestaoacoes.dto.LancamentoRequest;
import br.com.gestaoacoes.dto.PosicaoResponse;
import br.com.gestaoacoes.exception.RecursoNaoEncontradoException;
import br.com.gestaoacoes.model.Acao;
import br.com.gestaoacoes.model.Corretora;
import br.com.gestaoacoes.model.Lancamento;
import br.com.gestaoacoes.model.Mercado;
import br.com.gestaoacoes.model.Moeda;
import br.com.gestaoacoes.repository.AcaoRepository;
import br.com.gestaoacoes.repository.CorretoraRepository;
import br.com.gestaoacoes.repository.LancamentoRepository;
import br.com.gestaoacoes.repository.PosicaoAgregada;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CarteiraServiceTest {

    @Mock
    private LancamentoRepository lancamentoRepository;
    @Mock
    private AcaoRepository acaoRepository;
    @Mock
    private CorretoraRepository corretoraRepository;

    private CarteiraService service() {
        return new CarteiraService(lancamentoRepository, acaoRepository, corretoraRepository);
    }

    @Test
    void registrarLancamentoComSucessoPersisteAssociadoAoUsuario() {
        CarteiraService service = service();
        Acao acao = acao("PETR4", new BigDecimal("35.0000"));
        Corretora corretora = corretora();
        LancamentoRequest request = new LancamentoRequest(1L, 1L, new BigDecimal("100"), new BigDecimal("32.50"), LocalDate.now());
        when(acaoRepository.findById(1L)).thenReturn(Optional.of(acao));
        when(corretoraRepository.findById(1L)).thenReturn(Optional.of(corretora));
        when(lancamentoRepository.save(any(Lancamento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Lancamento lancamento = service.registrarLancamento(42L, request);

        assertThat(lancamento.getUsuarioId()).isEqualTo(42L);
        assertThat(lancamento.getAcao()).isEqualTo(acao);
        assertThat(lancamento.getCorretora()).isEqualTo(corretora);
    }

    @Test
    void registrarLancamentoComAcaoInexistenteLancaExcecao() {
        CarteiraService service = service();
        LancamentoRequest request = new LancamentoRequest(99L, 1L, new BigDecimal("100"), new BigDecimal("32.50"), LocalDate.now());
        when(acaoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrarLancamento(42L, request))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(lancamentoRepository, never()).save(any());
    }

    @Test
    void registrarLancamentoComCorretoraInexistenteLancaExcecao() {
        CarteiraService service = service();
        LancamentoRequest request = new LancamentoRequest(1L, 99L, new BigDecimal("100"), new BigDecimal("32.50"), LocalDate.now());
        when(acaoRepository.findById(1L)).thenReturn(Optional.of(acao("PETR4", new BigDecimal("35.0000"))));
        when(corretoraRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrarLancamento(42L, request))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(lancamentoRepository, never()).save(any());
    }

    @Test
    void listarPosicoesCalculaPrecoMedioValorAtualEVariacao() {
        CarteiraService service = service();
        Acao acao = acao("PETR4", new BigDecimal("40.0000"));
        PosicaoAgregada agregada = new PosicaoAgregada(acao, new BigDecimal("100"), new BigDecimal("3250.0000"));
        when(lancamentoRepository.agregarPosicoesPorUsuario(42L)).thenReturn(List.of(agregada));

        List<PosicaoResponse> posicoes = service.listarPosicoes(42L);

        assertThat(posicoes).hasSize(1);
        PosicaoResponse posicao = posicoes.get(0);
        assertThat(posicao.ticker()).isEqualTo("PETR4");
        assertThat(posicao.precoMedio()).isEqualByComparingTo("32.5000");
        assertThat(posicao.valorInvestido()).isEqualByComparingTo("3250.0000");
        assertThat(posicao.valorAtual()).isEqualByComparingTo("4000.0000");
        assertThat(posicao.variacaoPercentual()).isEqualByComparingTo("23.0769");
    }

    @Test
    void listarPosicoesDeCarteiraVaziaRetornaListaVazia() {
        CarteiraService service = service();
        when(lancamentoRepository.agregarPosicoesPorUsuario(42L)).thenReturn(List.of());

        assertThat(service.listarPosicoes(42L)).isEmpty();
    }

    private Acao acao(String ticker, BigDecimal cotacao) {
        return new Acao(ticker, "Empresa Teste", Mercado.BRASIL, Moeda.BRL, cotacao,
                OffsetDateTime.now(), "teste", OffsetDateTime.now());
    }

    private Corretora corretora() {
        return new Corretora("11222333000181", "Razao LTDA", "Fantasia", "contato@corretora.com", "1130000000",
                "01310100", "Av. Paulista", "1000", null, "Bela Vista", "Sao Paulo", "SP",
                "ATIVA", true, OffsetDateTime.now());
    }
}