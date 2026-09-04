package br.com.gestaoacoes.service;

import br.com.gestaoacoes.dto.LancamentoRequest;
import br.com.gestaoacoes.dto.PosicaoResponse;
import br.com.gestaoacoes.exception.RecursoNaoEncontradoException;
import br.com.gestaoacoes.exception.SaldoInsuficienteException;
import br.com.gestaoacoes.integration.cotacao.CambioPort;
import br.com.gestaoacoes.model.Acao;
import br.com.gestaoacoes.model.Corretora;
import br.com.gestaoacoes.model.Lancamento;
import br.com.gestaoacoes.model.Mercado;
import br.com.gestaoacoes.model.Moeda;
import br.com.gestaoacoes.model.TipoLancamento;
import br.com.gestaoacoes.repository.AcaoRepository;
import br.com.gestaoacoes.repository.CorretoraRepository;
import br.com.gestaoacoes.repository.LancamentoRepository;
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
import static org.mockito.ArgumentMatchers.anyLong;
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
    @Mock
    private CambioPort cambioPort;

    private CarteiraService service() {
        return new CarteiraService(lancamentoRepository, acaoRepository, corretoraRepository, cambioPort);
    }

    @Test
    void registrarLancamentoComSucessoPersisteAssociadoAoUsuario() {
        CarteiraService service = service();
        Acao acao = acao("PETR4", new BigDecimal("35.0000"));
        Corretora corretora = corretora();
        LancamentoRequest request = compra(1L, 1L, "100", "32.50");
        when(acaoRepository.findById(1L)).thenReturn(Optional.of(acao));
        when(corretoraRepository.findById(1L)).thenReturn(Optional.of(corretora));
        when(lancamentoRepository.save(any(Lancamento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Lancamento lancamento = service.registrarLancamento(42L, request);

        assertThat(lancamento.getUsuarioId()).isEqualTo(42L);
        assertThat(lancamento.getAcao()).isEqualTo(acao);
        assertThat(lancamento.getCorretora()).isEqualTo(corretora);
        assertThat(lancamento.getTipo()).isEqualTo(TipoLancamento.COMPRA);
    }

    @Test
    void registrarLancamentoComAcaoInexistenteLancaExcecao() {
        CarteiraService service = service();
        LancamentoRequest request = compra(99L, 1L, "100", "32.50");
        when(acaoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrarLancamento(42L, request))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(lancamentoRepository, never()).save(any());
    }

    @Test
    void registrarLancamentoComCorretoraInexistenteLancaExcecao() {
        CarteiraService service = service();
        LancamentoRequest request = compra(1L, 99L, "100", "32.50");
        when(acaoRepository.findById(1L)).thenReturn(Optional.of(acao("PETR4", new BigDecimal("35.0000"))));
        when(corretoraRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrarLancamento(42L, request))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(lancamentoRepository, never()).save(any());
    }

    @Test
    void registrarVendaDentroDoSaldoPersisteComTipoVenda() {
        CarteiraService service = service();
        Acao acao = acao("PETR4", new BigDecimal("35.0000"));
        Corretora corretora = corretora();
        LancamentoRequest request = venda(1L, 1L, "40", "35.00");
        when(acaoRepository.findById(1L)).thenReturn(Optional.of(acao));
        when(corretoraRepository.findById(1L)).thenReturn(Optional.of(corretora));
        when(lancamentoRepository.findByUsuarioIdAndAcaoId(42L, null))
                .thenReturn(List.of(lancamento(42L, acao, corretora, TipoLancamento.COMPRA, "100", "32.50")));
        when(lancamentoRepository.save(any(Lancamento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Lancamento lancamento = service.registrarLancamento(42L, request);

        assertThat(lancamento.getTipo()).isEqualTo(TipoLancamento.VENDA);
    }

    @Test
    void registrarVendaAcimaDoSaldoLancaExcecaoENaoPersiste() {
        CarteiraService service = service();
        Acao acao = acao("PETR4", new BigDecimal("35.0000"));
        Corretora corretora = corretora();
        LancamentoRequest request = venda(1L, 1L, "150", "35.00");
        when(acaoRepository.findById(1L)).thenReturn(Optional.of(acao));
        when(corretoraRepository.findById(1L)).thenReturn(Optional.of(corretora));
        when(lancamentoRepository.findByUsuarioIdAndAcaoId(42L, null))
                .thenReturn(List.of(lancamento(42L, acao, corretora, TipoLancamento.COMPRA, "100", "32.50")));

        assertThatThrownBy(() -> service.registrarLancamento(42L, request))
                .isInstanceOf(SaldoInsuficienteException.class);
        verify(lancamentoRepository, never()).save(any());
    }

    @Test
    void registrarVendaDeAtivoSemPosicaoLancaExcecao() {
        CarteiraService service = service();
        Acao acao = acao("VALE3", new BigDecimal("60.0000"));
        Corretora corretora = corretora();
        LancamentoRequest request = venda(1L, 1L, "10", "60.00");
        when(acaoRepository.findById(1L)).thenReturn(Optional.of(acao));
        when(corretoraRepository.findById(1L)).thenReturn(Optional.of(corretora));
        when(lancamentoRepository.findByUsuarioIdAndAcaoId(42L, null)).thenReturn(List.of());

        assertThatThrownBy(() -> service.registrarLancamento(42L, request))
                .isInstanceOf(SaldoInsuficienteException.class);
        verify(lancamentoRepository, never()).save(any());
    }

    @Test
    void excluirLancamentoDoProprioUsuarioRemove() {
        CarteiraService service = service();
        Lancamento lancamento = lancamento(42L, acao("PETR4", new BigDecimal("35.0000")), corretora(),
                TipoLancamento.COMPRA, "100", "32.50");
        when(lancamentoRepository.findById(5L)).thenReturn(Optional.of(lancamento));

        service.excluirLancamento(42L, 5L);

        verify(lancamentoRepository).delete(lancamento);
    }

    @Test
    void excluirLancamentoInexistenteLancaExcecao() {
        CarteiraService service = service();
        when(lancamentoRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.excluirLancamento(42L, 5L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(lancamentoRepository, never()).delete(any());
    }

    @Test
    void excluirLancamentoDeOutroUsuarioLancaExcecaoENaoRemove() {
        CarteiraService service = service();
        Lancamento lancamento = lancamento(99L, acao("PETR4", new BigDecimal("35.0000")), corretora(),
                TipoLancamento.COMPRA, "100", "32.50");
        when(lancamentoRepository.findById(5L)).thenReturn(Optional.of(lancamento));

        assertThatThrownBy(() -> service.excluirLancamento(42L, 5L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(lancamentoRepository, never()).delete(any());
    }

    @Test
    void listarPosicoesComUmaUnicaCompraCalculaPrecoMedioValorAtualEVariacao() {
        CarteiraService service = service();
        Acao acao = acao("PETR4", new BigDecimal("40.0000"));
        Corretora corretora = corretora();
        when(lancamentoRepository.listarPorUsuarioOrdenadoPorAtivoEData(42L)).thenReturn(List.of(
                lancamento(42L, acao, corretora, TipoLancamento.COMPRA, "100", "32.50")));

        List<PosicaoResponse> posicoes = service.listarPosicoes(42L);

        assertThat(posicoes).hasSize(1);
        PosicaoResponse posicao = posicoes.get(0);
        assertThat(posicao.ticker()).isEqualTo("PETR4");
        assertThat(posicao.quantidade()).isEqualByComparingTo("100");
        assertThat(posicao.precoMedio()).isEqualByComparingTo("32.5000");
        assertThat(posicao.valorInvestido()).isEqualByComparingTo("3250.0000");
        assertThat(posicao.valorAtual()).isEqualByComparingTo("4000.0000");
        assertThat(posicao.variacaoPercentual()).isEqualByComparingTo("23.0769");
        assertThat(posicao.resultadoRealizado()).isEqualByComparingTo("0.0000");
    }

    @Test
    void listarPosicoesComMultiplasComprasCalculaPrecoMedioPonderado() {
        CarteiraService service = service();
        Acao acao = acao("PETR4", new BigDecimal("40.0000"));
        Corretora corretora = corretora();
        when(lancamentoRepository.listarPorUsuarioOrdenadoPorAtivoEData(42L)).thenReturn(List.of(
                lancamento(42L, acao, corretora, TipoLancamento.COMPRA, "100", "30.00"),
                lancamento(42L, acao, corretora, TipoLancamento.COMPRA, "50", "36.00")));

        List<PosicaoResponse> posicoes = service.listarPosicoes(42L);

        assertThat(posicoes).hasSize(1);
        PosicaoResponse posicao = posicoes.get(0);
        assertThat(posicao.quantidade()).isEqualByComparingTo("150");
        assertThat(posicao.precoMedio()).isEqualByComparingTo("32.0000");
        assertThat(posicao.valorInvestido()).isEqualByComparingTo("4800.0000");
    }

    @Test
    void vendaParcialMantemPrecoMedioERealizaResultado() {
        CarteiraService service = service();
        Acao acao = acao("PETR4", new BigDecimal("40.0000"));
        Corretora corretora = corretora();
        when(lancamentoRepository.listarPorUsuarioOrdenadoPorAtivoEData(42L)).thenReturn(List.of(
                lancamento(42L, acao, corretora, TipoLancamento.COMPRA, "100", "32.50"),
                lancamento(42L, acao, corretora, TipoLancamento.VENDA, "40", "35.00")));

        List<PosicaoResponse> posicoes = service.listarPosicoes(42L);

        assertThat(posicoes).hasSize(1);
        PosicaoResponse posicao = posicoes.get(0);
        assertThat(posicao.quantidade()).isEqualByComparingTo("60");
        assertThat(posicao.precoMedio()).isEqualByComparingTo("32.5000");
        assertThat(posicao.resultadoRealizado()).isEqualByComparingTo("100.0000");
    }

    @Test
    void vendaTotalZeraQuantidadeERemovePosicaoDaListagem() {
        CarteiraService service = service();
        Acao acao = acao("PETR4", new BigDecimal("40.0000"));
        Corretora corretora = corretora();
        when(lancamentoRepository.listarPorUsuarioOrdenadoPorAtivoEData(42L)).thenReturn(List.of(
                lancamento(42L, acao, corretora, TipoLancamento.COMPRA, "100", "32.50"),
                lancamento(42L, acao, corretora, TipoLancamento.VENDA, "100", "35.00")));

        List<PosicaoResponse> posicoes = service.listarPosicoes(42L);

        assertThat(posicoes).isEmpty();
    }

    @Test
    void compraAposVendaParcialRecalculaPrecoMedioSobreSaldoRemanescente() {
        CarteiraService service = service();
        Acao acao = acao("PETR4", new BigDecimal("40.0000"));
        Corretora corretora = corretora();
        when(lancamentoRepository.listarPorUsuarioOrdenadoPorAtivoEData(42L)).thenReturn(List.of(
                lancamento(42L, acao, corretora, TipoLancamento.COMPRA, "100", "32.50"),
                lancamento(42L, acao, corretora, TipoLancamento.VENDA, "40", "35.00"),
                lancamento(42L, acao, corretora, TipoLancamento.COMPRA, "20", "40.00")));

        List<PosicaoResponse> posicoes = service.listarPosicoes(42L);

        assertThat(posicoes).hasSize(1);
        PosicaoResponse posicao = posicoes.get(0);
        // 60 remanescentes a 32,50 (custo 1950) + 20 novas a 40,00 (custo 800) = 2750 / 80 = 34,375
        assertThat(posicao.quantidade()).isEqualByComparingTo("80");
        assertThat(posicao.precoMedio()).isEqualByComparingTo("34.3750");
        assertThat(posicao.resultadoRealizado()).isEqualByComparingTo("100.0000");
    }

    @Test
    void listarPosicoesComMultiplosAtivosNaoMisturaCalculos() {
        CarteiraService service = service();
        Acao petr4 = acao("PETR4", new BigDecimal("40.0000"));
        Acao vale3 = acao("VALE3", new BigDecimal("70.0000"));
        Corretora corretora = corretora();
        when(lancamentoRepository.listarPorUsuarioOrdenadoPorAtivoEData(42L)).thenReturn(List.of(
                lancamento(42L, petr4, corretora, TipoLancamento.COMPRA, "100", "32.50"),
                lancamento(42L, vale3, corretora, TipoLancamento.COMPRA, "10", "60.00")));

        List<PosicaoResponse> posicoes = service.listarPosicoes(42L);

        assertThat(posicoes).hasSize(2);
        assertThat(posicoes).extracting(PosicaoResponse::ticker).containsExactlyInAnyOrder("PETR4", "VALE3");
    }

    @Test
    void listarPosicoesDeCarteiraVaziaRetornaListaVazia() {
        CarteiraService service = service();
        when(lancamentoRepository.listarPorUsuarioOrdenadoPorAtivoEData(42L)).thenReturn(List.of());

        assertThat(service.listarPosicoes(42L)).isEmpty();
        verify(cambioPort, never()).obterCotacaoUsdParaBrl();
    }

    @Test
    void listarPosicoesEmUsdConverteParaBrlUsandoCambioAtualEMantemVariacao() {
        CarteiraService service = service();
        Acao acao = acaoUsd("AAPL", new BigDecimal("300.0000"));
        Corretora corretora = corretora();
        when(lancamentoRepository.listarPorUsuarioOrdenadoPorAtivoEData(42L)).thenReturn(List.of(
                lancamento(42L, acao, corretora, TipoLancamento.COMPRA, "2", "15.00")));
        when(cambioPort.obterCotacaoUsdParaBrl()).thenReturn(new BigDecimal("5.0000"));

        List<PosicaoResponse> posicoes = service.listarPosicoes(42L);

        assertThat(posicoes).hasSize(1);
        PosicaoResponse posicao = posicoes.get(0);
        // Nativo (USD): precoMedio 15.00, valorInvestido 30.00, valorAtual 600.00 -> convertido x5.
        assertThat(posicao.precoMedio()).isEqualByComparingTo("75.0000");
        assertThat(posicao.valorInvestido()).isEqualByComparingTo("150.0000");
        assertThat(posicao.valorAtual()).isEqualByComparingTo("3000.0000");
        assertThat(posicao.variacaoPercentual()).isEqualByComparingTo("1900.0000");
    }

    @Test
    void listarPosicoesSoEmBrlNaoConsultaCambio() {
        CarteiraService service = service();
        Acao acao = acao("PETR4", new BigDecimal("40.0000"));
        Corretora corretora = corretora();
        when(lancamentoRepository.listarPorUsuarioOrdenadoPorAtivoEData(42L)).thenReturn(List.of(
                lancamento(42L, acao, corretora, TipoLancamento.COMPRA, "100", "32.50")));

        service.listarPosicoes(42L);

        verify(cambioPort, never()).obterCotacaoUsdParaBrl();
    }

    private LancamentoRequest compra(Long acaoId, Long corretoraId, String quantidade, String preco) {
        return new LancamentoRequest(acaoId, corretoraId, TipoLancamento.COMPRA, new BigDecimal(quantidade),
                new BigDecimal(preco), LocalDate.now());
    }

    private LancamentoRequest venda(Long acaoId, Long corretoraId, String quantidade, String preco) {
        return new LancamentoRequest(acaoId, corretoraId, TipoLancamento.VENDA, new BigDecimal(quantidade),
                new BigDecimal(preco), LocalDate.now());
    }

    private Lancamento lancamento(Long usuarioId, Acao acao, Corretora corretora, TipoLancamento tipo,
                                   String quantidade, String preco) {
        return new Lancamento(usuarioId, acao, corretora, tipo, new BigDecimal(quantidade), new BigDecimal(preco),
                LocalDate.now(), OffsetDateTime.now());
    }

    private Acao acao(String ticker, BigDecimal cotacao) {
        return new Acao(ticker, "Empresa Teste", Mercado.BRASIL, Moeda.BRL, cotacao,
                OffsetDateTime.now(), "teste", OffsetDateTime.now());
    }

    private Acao acaoUsd(String ticker, BigDecimal cotacao) {
        return new Acao(ticker, "Empresa Teste", Mercado.ESTADOS_UNIDOS, Moeda.USD, cotacao,
                OffsetDateTime.now(), "teste", OffsetDateTime.now());
    }

    private Corretora corretora() {
        return new Corretora("11222333000181", "Razao LTDA", "Fantasia", "contato@corretora.com", "1130000000",
                "01310100", "Av. Paulista", "1000", null, "Bela Vista", "Sao Paulo", "SP",
                "ATIVA", true, OffsetDateTime.now());
    }
}
