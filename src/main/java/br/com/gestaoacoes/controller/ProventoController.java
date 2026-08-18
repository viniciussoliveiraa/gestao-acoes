package br.com.gestaoacoes.controller;

import br.com.gestaoacoes.dto.ProventoRequest;
import br.com.gestaoacoes.dto.ProventoResponse;
import br.com.gestaoacoes.mapper.ProventoMapper;
import br.com.gestaoacoes.model.Provento;
import br.com.gestaoacoes.service.ProventoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/proventos")
public class ProventoController {

    private final ProventoService service;
    private final ProventoMapper mapper;

    public ProventoController(ProventoService service, ProventoMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProventoResponse registrar(@AuthenticationPrincipal Long usuarioId,
                                       @Valid @RequestBody ProventoRequest request) {
        Provento provento = service.registrar(usuarioId, request);
        return mapper.toResponse(provento);
    }

    @GetMapping
    public Page<ProventoResponse> listar(@AuthenticationPrincipal Long usuarioId, Pageable pageable) {
        return service.listar(usuarioId, pageable).map(mapper::toResponse);
    }
}