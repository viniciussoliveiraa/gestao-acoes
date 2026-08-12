package br.com.gestaoacoes.service;

import br.com.gestaoacoes.dto.AcaoRequest;
import br.com.gestaoacoes.exception.AcaoDuplicadaException;
import br.com.gestaoacoes.exception.IntegracaoExternaIndisponivelException;
import br.com.gestaoacoes.exception.RecursoNaoEncontradoException;
import br.com.gestaoacoes.exception.TickerAmbiguoException;
import br.com.gestaoacoes.exception.TickerNaoEncontradoException;
import br.com.gestaoacoes.integration.cotacao.CotacaoExterna;
import br.com.gestaoacoes.integration.cotacao.CotacaoPort;
import br.com.gestaoacoes.integration.cotacao.CotacaoStrategyResolver;
import br.com.gestaoacoes.model.Acao;
import br.com.gestaoacoes.model.Mercado;
import br.com.gestaoacoes.model.Moeda;
import br.com.gestaoacoes.repository.AcaoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
class AcaoServiceTest {

    @Mock
    private AcaoRepository repository;
    @Mock
    private CotacaoStrategyResolver strategyResolver;
    @Mock
    private CotacaoPort cotacaoPort;

    private AcaoService service() {
        return new AcaoService(repository, strategyResolver);
    }

    @Test
    void registrarComSucessoPersisteAcao() {
        AcaoService service = service();
        AcaoRequest request = new AcaoRequest(" petr4 ", Mercado.BRASIL);
        when(repository.findByTickerAndMercado("PETR4", Mercado.BRASIL)).thenReturn(Optional.empty());
        when(strategyResolver.resolver(Mercado.BRASIL)).thenReturn(cotacaoPort);
        when(cotacaoPort.obterCotacao("PETR4")).thenReturn(new CotacaoExterna(
                "Petrobras", Moeda.BRL, new BigDecimal("38.4200"), OffsetDateTime.now(), "brapi"));
        when(repository.save(any(Acao.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Acao acao = service.registrar(request);

        assertThat(acao.getTicker()).isEqualTo("PETR4");
        assertThat(acao.getCotacaoAtual()).isEqualByComparingTo("38.4200");
    }

    @Test
    void tickerEMercadoDuplicadoNaoConsultaCotacao() {
        AcaoService service = service();
        AcaoRequest request = new AcaoRequest("PETR4", Mercado.BRASIL);
        when(repository.findByTickerAndMercado("PETR4", Mercado.BRASIL))
                .thenReturn(Optional.of(mockAcao()));

        assertThatThrownBy(() -> service.registrar(request)).isInstanceOf(AcaoDuplicadaException.class);

        verify(strategyResolver, never()).resolver(any());
    }

    @Test
    void tickerNaoEncontradoNaoPersisteNada() {
        AcaoService service = service();
        AcaoRequest request = new AcaoRequest("ZZZZ9", Mercado.BRASIL);
        when(repository.findByTickerAndMercado("ZZZZ9", Mercado.BRASIL)).thenReturn(Optional.empty());
        when(strategyResolver.resolver(Mercado.BRASIL)).thenReturn(cotacaoPort);
        when(cotacaoPort.obterCotacao("ZZZZ9")).thenThrow(new TickerNaoEncontradoException("não encontrado"));

        assertThatThrownBy(() -> service.registrar(request)).isInstanceOf(TickerNaoEncontradoException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void atualizarCotacaoComSucessoAtualizaValores() {
        AcaoService service = service();
        Acao acao = mockAcao();
        when(repository.findById(1L)).thenReturn(Optional.of(acao));
        when(strategyResolver.resolver(Mercado.BRASIL)).thenReturn(cotacaoPort);
        OffsetDateTime novaData = OffsetDateTime.now();
        when(cotacaoPort.obterCotacao("PETR4")).thenReturn(new CotacaoExterna(
                "Petrobras", Moeda.BRL, new BigDecimal("40.0000"), novaData, "brapi"));
        when(repository.save(acao)).thenReturn(acao);

        Acao atualizada = service.atualizarCotacao(1L);

        assertThat(atualizada.getCotacaoAtual()).isEqualByComparingTo("40.0000");
        assertThat(atualizada.getDataHoraCotacao()).isEqualTo(novaData);
    }

    @Test
    void falhaNaAtualizacaoMantemUltimaCotacaoValida() {
        AcaoService service = service();
        Acao acao = mockAcao();
        BigDecimal cotacaoOriginal = acao.getCotacaoAtual();
        when(repository.findById(1L)).thenReturn(Optional.of(acao));
        when(strategyResolver.resolver(Mercado.BRASIL)).thenReturn(cotacaoPort);
        when(cotacaoPort.obterCotacao("PETR4")).thenThrow(new IntegracaoExternaIndisponivelException("indisponível"));

        assertThatThrownBy(() -> service.atualizarCotacao(1L)).isInstanceOf(IntegracaoExternaIndisponivelException.class);

        assertThat(acao.getCotacaoAtual()).isEqualByComparingTo(cotacaoOriginal);
        verify(repository, never()).save(any());
    }

    @Test
    void buscarPorTickerAmbiguoSemMercadoLancaExcecao() {
        AcaoService service = service();
        when(repository.findByTicker("IBM")).thenReturn(List.of(
                mockAcao(), acaoComMercado(Mercado.ESTADOS_UNIDOS)));

        assertThatThrownBy(() -> service.buscarPorTicker("IBM", null)).isInstanceOf(TickerAmbiguoException.class);
    }

    @Test
    void buscarPorTickerInexistenteLancaRecursoNaoEncontrado() {
        AcaoService service = service();
        when(repository.findByTicker("ZZZZ9")).thenReturn(List.of());

        assertThatThrownBy(() -> service.buscarPorTicker("ZZZZ9", null)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    private Acao mockAcao() {
        return new Acao("PETR4", "Petrobras", Mercado.BRASIL, Moeda.BRL, new BigDecimal("38.4200"),
                OffsetDateTime.now(), "brapi", OffsetDateTime.now());
    }

    private Acao acaoComMercado(Mercado mercado) {
        Moeda moeda = mercado == Mercado.BRASIL ? Moeda.BRL : Moeda.USD;
        return new Acao("IBM", "IBM Corp", mercado, moeda, new BigDecimal("100.0000"),
                OffsetDateTime.now(), "teste", OffsetDateTime.now());
    }
}