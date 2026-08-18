package br.com.gestaoacoes.service;

import br.com.gestaoacoes.dto.ProventoRequest;
import br.com.gestaoacoes.exception.RecursoNaoEncontradoException;
import br.com.gestaoacoes.model.Acao;
import br.com.gestaoacoes.model.Provento;
import br.com.gestaoacoes.repository.AcaoRepository;
import br.com.gestaoacoes.repository.ProventoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class ProventoService {

    private final ProventoRepository repository;
    private final AcaoRepository acaoRepository;

    public ProventoService(ProventoRepository repository, AcaoRepository acaoRepository) {
        this.repository = repository;
        this.acaoRepository = acaoRepository;
    }

    public Provento registrar(Long usuarioId, ProventoRequest request) {
        Acao acao = acaoRepository.findById(request.acaoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ação não encontrada: id " + request.acaoId()));

        Provento provento = new Provento(usuarioId, acao, request.tipo(), request.valorTotal(),
                request.dataPagamento(), OffsetDateTime.now());

        return repository.save(provento);
    }

    public Page<Provento> listar(Long usuarioId, Pageable pageable) {
        return repository.findByUsuarioIdOrderByDataPagamentoDesc(usuarioId, pageable);
    }
}