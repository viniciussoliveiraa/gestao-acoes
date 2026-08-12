package br.com.gestaoacoes.service;

import br.com.gestaoacoes.dto.AcaoRequest;
import br.com.gestaoacoes.exception.AcaoDuplicadaException;
import br.com.gestaoacoes.exception.RecursoNaoEncontradoException;
import br.com.gestaoacoes.integration.cotacao.CotacaoExterna;
import br.com.gestaoacoes.integration.cotacao.CotacaoPort;
import br.com.gestaoacoes.integration.cotacao.CotacaoStrategyResolver;
import br.com.gestaoacoes.model.Acao;
import br.com.gestaoacoes.model.Mercado;
import br.com.gestaoacoes.repository.AcaoRepository;
import br.com.gestaoacoes.util.TickerUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class AcaoService {

    private final AcaoRepository repository;
    private final CotacaoStrategyResolver cotacaoStrategyResolver;

    public AcaoService(AcaoRepository repository, CotacaoStrategyResolver cotacaoStrategyResolver) {
        this.repository = repository;
        this.cotacaoStrategyResolver = cotacaoStrategyResolver;
    }

    public Acao registrar(AcaoRequest request) {
        String ticker = TickerUtils.normalizar(request.ticker());
        Mercado mercado = request.mercado();

        if (repository.findByTicker(ticker).isPresent()) {
            throw new AcaoDuplicadaException("Já existe uma ação cadastrada com este ticker");
        }

        CotacaoPort cotacaoPort = cotacaoStrategyResolver.resolver(mercado);
        CotacaoExterna cotacao = cotacaoPort.obterCotacao(ticker);

        Acao acao = new Acao(
                ticker,
                cotacao.nomeEmpresa(),
                mercado,
                cotacao.moeda(),
                cotacao.preco(),
                cotacao.dataHora(),
                cotacao.provedor(),
                OffsetDateTime.now()
        );

        return repository.save(acao);
    }

    public Acao atualizarCotacao(Long id) {
        Acao acao = buscarPorId(id);
        CotacaoPort cotacaoPort = cotacaoStrategyResolver.resolver(acao.getMercado());
        CotacaoExterna cotacao = cotacaoPort.obterCotacao(acao.getTicker());
        acao.atualizarCotacao(cotacao.preco(), cotacao.dataHora());
        return repository.save(acao);
    }

    public Page<Acao> listar(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Acao buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ação não encontrada: id " + id));
    }

    public Acao buscarPorTicker(String ticker) {
        String tickerNormalizado = TickerUtils.normalizar(ticker);
        return repository.findByTicker(tickerNormalizado)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ação não encontrada para o ticker informado"));
    }
}