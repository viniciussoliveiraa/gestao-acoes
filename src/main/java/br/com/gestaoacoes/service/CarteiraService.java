package br.com.gestaoacoes.service;

import br.com.gestaoacoes.dto.LancamentoRequest;
import br.com.gestaoacoes.dto.PosicaoResponse;
import br.com.gestaoacoes.exception.RecursoNaoEncontradoException;
import br.com.gestaoacoes.exception.SaldoInsuficienteException;
import br.com.gestaoacoes.integration.cotacao.CambioPort;
import br.com.gestaoacoes.model.Acao;
import br.com.gestaoacoes.model.Corretora;
import br.com.gestaoacoes.model.Lancamento;
import br.com.gestaoacoes.model.Moeda;
import br.com.gestaoacoes.model.TipoLancamento;
import br.com.gestaoacoes.repository.AcaoRepository;
import br.com.gestaoacoes.repository.CorretoraRepository;
import br.com.gestaoacoes.repository.LancamentoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CarteiraService {

    private static final int ESCALA_MONETARIA = 4;
    // Escala maior para o preço médio corrente durante o acúmulo, para não perder precisão a
    // cada recompra sucessiva antes de arredondar o valor final exibido.
    private static final int ESCALA_PRECO_MEDIO_INTERNO = 10;

    private final LancamentoRepository lancamentoRepository;
    private final AcaoRepository acaoRepository;
    private final CorretoraRepository corretoraRepository;
    private final CambioPort cambioPort;

    public CarteiraService(LancamentoRepository lancamentoRepository, AcaoRepository acaoRepository,
                            CorretoraRepository corretoraRepository, CambioPort cambioPort) {
        this.lancamentoRepository = lancamentoRepository;
        this.acaoRepository = acaoRepository;
        this.corretoraRepository = corretoraRepository;
        this.cambioPort = cambioPort;
    }

    public Lancamento registrarLancamento(Long usuarioId, LancamentoRequest request) {
        Acao acao = acaoRepository.findById(request.acaoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ação não encontrada: id " + request.acaoId()));
        Corretora corretora = corretoraRepository.findById(request.corretoraId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Corretora não encontrada: id " + request.corretoraId()));

        TipoLancamento tipo = request.tipoOuPadrao();
        if (tipo == TipoLancamento.VENDA) {
            BigDecimal saldoAtual = calcularQuantidadeLiquida(usuarioId, acao.getId());
            if (request.quantidade().compareTo(saldoAtual) > 0) {
                throw new SaldoInsuficienteException("Saldo insuficiente de " + acao.getTicker()
                        + " para venda: disponível " + saldoAtual + ", solicitado " + request.quantidade());
            }
        }

        Lancamento lancamento = new Lancamento(usuarioId, acao, corretora, tipo, request.quantidade(),
                request.precoUnitario(), request.dataOperacao(), OffsetDateTime.now());

        return lancamentoRepository.save(lancamento);
    }

    public Page<Lancamento> listarLancamentos(Long usuarioId, Pageable pageable) {
        return lancamentoRepository.findByUsuarioId(usuarioId, pageable);
    }

    public List<PosicaoResponse> listarPosicoes(Long usuarioId) {
        List<Lancamento> lancamentos = lancamentoRepository.listarPorUsuarioOrdenadoPorAtivoEData(usuarioId);

        // Agrupa pela própria entidade Acao (identidade de objeto, sem equals/hashCode
        // sobrescritos): dentro de uma mesma consulta/sessão Hibernate garante a mesma instância
        // para o mesmo acao_id (identity map do JOIN FETCH), então não precisa depender do id
        // (que também poderia ser nulo para entidades transitórias em teste).
        Map<Acao, PosicaoAcumulador> acumuladores = new LinkedHashMap<>();
        for (Lancamento lancamento : lancamentos) {
            acumuladores.computeIfAbsent(lancamento.getAcao(), PosicaoAcumulador::new).aplicar(lancamento);
        }

        List<PosicaoAcumulador> posicoesAbertas = acumuladores.values().stream()
                .filter(acumulador -> acumulador.quantidade.signum() > 0)
                .toList();

        // Busca o câmbio uma única vez por chamada (não por posição) e só quando há
        // ao menos um ativo em USD na carteira.
        boolean temAtivoEmUsd = posicoesAbertas.stream().anyMatch(a -> a.acao.getMoeda() == Moeda.USD);
        BigDecimal taxaCambioUsdParaBrl = temAtivoEmUsd ? cambioPort.obterCotacaoUsdParaBrl() : null;

        return posicoesAbertas.stream()
                .map(acumulador -> paraPosicaoResponse(acumulador, taxaCambioUsdParaBrl))
                .toList();
    }

    private BigDecimal calcularQuantidadeLiquida(Long usuarioId, Long acaoId) {
        List<Lancamento> lancamentosDoAtivo = lancamentoRepository.findByUsuarioIdAndAcaoId(usuarioId, acaoId);
        BigDecimal quantidade = BigDecimal.ZERO;
        for (Lancamento lancamento : lancamentosDoAtivo) {
            quantidade = lancamento.getTipo() == TipoLancamento.VENDA
                    ? quantidade.subtract(lancamento.getQuantidade())
                    : quantidade.add(lancamento.getQuantidade());
        }
        return quantidade;
    }

    private PosicaoResponse paraPosicaoResponse(PosicaoAcumulador acumulador, BigDecimal taxaCambioUsdParaBrl) {
        Acao acao = acumulador.acao;
        BigDecimal quantidade = acumulador.quantidade;
        BigDecimal precoMedio = acumulador.precoMedio.setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);
        BigDecimal valorInvestido = quantidade.multiply(precoMedio).setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);
        BigDecimal valorAtual = quantidade.multiply(acao.getCotacaoAtual()).setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);
        BigDecimal resultadoRealizado = acumulador.resultadoRealizado.setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);
        // Multiplica por 100 antes de dividir para não perder precisão arredondando o quociente
        // duas vezes (ver CarteiraServiceTest).
        BigDecimal variacaoPercentual = valorInvestido.signum() == 0
                ? BigDecimal.ZERO
                : valorAtual.subtract(valorInvestido)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(valorInvestido, ESCALA_MONETARIA, RoundingMode.HALF_UP);

        if (acao.getMoeda() == Moeda.USD) {
            // Converte para BRL com o câmbio atual — como o mesmo fator multiplica
            // valorInvestido e valorAtual, a variação percentual não é afetada.
            valorInvestido = valorInvestido.multiply(taxaCambioUsdParaBrl).setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);
            precoMedio = precoMedio.multiply(taxaCambioUsdParaBrl).setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);
            valorAtual = valorAtual.multiply(taxaCambioUsdParaBrl).setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);
            resultadoRealizado = resultadoRealizado.multiply(taxaCambioUsdParaBrl).setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);
        }

        return new PosicaoResponse(acao.getId(), acao.getTicker(), acao.getNomeEmpresa(), quantidade,
                precoMedio, valorInvestido, valorAtual, variacaoPercentual, resultadoRealizado);
    }

    /**
     * Acumula, ao processar os lançamentos de um ativo em ordem cronológica, a posição pelo
     * método de custo médio ponderado: compra recalcula o preço médio ponderando pela
     * quantidade comprada; venda reduz a quantidade sem alterar o preço médio e realiza o
     * resultado (preço de venda − preço médio no momento) × quantidade vendida.
     */
    private static final class PosicaoAcumulador {
        private final Acao acao;
        private BigDecimal quantidade = BigDecimal.ZERO;
        private BigDecimal precoMedio = BigDecimal.ZERO;
        private BigDecimal resultadoRealizado = BigDecimal.ZERO;

        private PosicaoAcumulador(Acao acao) {
            this.acao = acao;
        }

        private void aplicar(Lancamento lancamento) {
            BigDecimal quantidadeLancamento = lancamento.getQuantidade();
            BigDecimal precoLancamento = lancamento.getPrecoUnitario();

            if (lancamento.getTipo() == TipoLancamento.VENDA) {
                resultadoRealizado = resultadoRealizado.add(
                        precoLancamento.subtract(precoMedio).multiply(quantidadeLancamento));
                quantidade = quantidade.subtract(quantidadeLancamento);
                return;
            }

            BigDecimal custoAnterior = quantidade.multiply(precoMedio);
            BigDecimal custoNovo = quantidadeLancamento.multiply(precoLancamento);
            quantidade = quantidade.add(quantidadeLancamento);
            precoMedio = custoAnterior.add(custoNovo).divide(quantidade, ESCALA_PRECO_MEDIO_INTERNO, RoundingMode.HALF_UP);
        }
    }
}
