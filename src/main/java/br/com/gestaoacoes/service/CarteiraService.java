package br.com.gestaoacoes.service;

import br.com.gestaoacoes.dto.LancamentoRequest;
import br.com.gestaoacoes.dto.PosicaoResponse;
import br.com.gestaoacoes.exception.RecursoNaoEncontradoException;
import br.com.gestaoacoes.integration.cotacao.CambioPort;
import br.com.gestaoacoes.model.Acao;
import br.com.gestaoacoes.model.Corretora;
import br.com.gestaoacoes.model.Lancamento;
import br.com.gestaoacoes.model.Moeda;
import br.com.gestaoacoes.repository.AcaoRepository;
import br.com.gestaoacoes.repository.CorretoraRepository;
import br.com.gestaoacoes.repository.LancamentoRepository;
import br.com.gestaoacoes.repository.PosicaoAgregada;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class CarteiraService {

    private static final int ESCALA_MONETARIA = 4;

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

        Lancamento lancamento = new Lancamento(usuarioId, acao, corretora, request.quantidade(),
                request.precoUnitario(), request.dataOperacao(), OffsetDateTime.now());

        return lancamentoRepository.save(lancamento);
    }

    public Page<Lancamento> listarLancamentos(Long usuarioId, Pageable pageable) {
        return lancamentoRepository.findByUsuarioId(usuarioId, pageable);
    }

    public List<PosicaoResponse> listarPosicoes(Long usuarioId) {
        List<PosicaoAgregada> agregadas = lancamentoRepository.agregarPosicoesPorUsuario(usuarioId);

        // Busca o câmbio uma única vez por chamada (não por posição) e só quando há
        // ao menos um ativo em USD na carteira.
        boolean temAtivoEmUsd = agregadas.stream().anyMatch(a -> a.acao().getMoeda() == Moeda.USD);
        BigDecimal taxaCambioUsdParaBrl = temAtivoEmUsd ? cambioPort.obterCotacaoUsdParaBrl() : null;

        return agregadas.stream()
                .map(agregada -> calcularPosicao(agregada, taxaCambioUsdParaBrl))
                .toList();
    }

    private PosicaoResponse calcularPosicao(PosicaoAgregada agregada, BigDecimal taxaCambioUsdParaBrl) {
        Acao acao = agregada.acao();
        BigDecimal quantidade = agregada.quantidadeTotal();
        BigDecimal valorInvestido = agregada.valorInvestidoTotal().setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);
        BigDecimal precoMedio = valorInvestido.divide(quantidade, ESCALA_MONETARIA, RoundingMode.HALF_UP);
        BigDecimal valorAtual = quantidade.multiply(acao.getCotacaoAtual()).setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);
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
        }

        return new PosicaoResponse(acao.getId(), acao.getTicker(), acao.getNomeEmpresa(), quantidade,
                precoMedio, valorInvestido, valorAtual, variacaoPercentual);
    }
}