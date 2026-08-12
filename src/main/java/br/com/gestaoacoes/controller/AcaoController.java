package br.com.gestaoacoes.controller;

import br.com.gestaoacoes.dto.AcaoRequest;
import br.com.gestaoacoes.dto.AcaoResponse;
import br.com.gestaoacoes.mapper.AcaoMapper;
import br.com.gestaoacoes.model.Acao;
import br.com.gestaoacoes.model.Mercado;
import br.com.gestaoacoes.service.AcaoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/acoes")
public class AcaoController {

    private final AcaoService service;
    private final AcaoMapper mapper;

    public AcaoController(AcaoService service, AcaoMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AcaoResponse registrar(@Valid @RequestBody AcaoRequest request) {
        Acao acao = service.registrar(request);
        return mapper.toResponse(acao);
    }

    @GetMapping
    public Page<AcaoResponse> listar(Pageable pageable) {
        return service.listar(pageable).map(mapper::toResponse);
    }

    @GetMapping("/{id}")
    public AcaoResponse buscarPorId(@PathVariable Long id) {
        return mapper.toResponse(service.buscarPorId(id));
    }

    @GetMapping("/ticker/{ticker}")
    public AcaoResponse buscarPorTicker(@PathVariable String ticker,
                                         @RequestParam(required = false) Mercado mercado) {
        return mapper.toResponse(service.buscarPorTicker(ticker, mercado));
    }

    @PutMapping("/{id}/atualizar-cotacao")
    public AcaoResponse atualizarCotacao(@PathVariable Long id) {
        return mapper.toResponse(service.atualizarCotacao(id));
    }
}