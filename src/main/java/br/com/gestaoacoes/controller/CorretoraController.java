package br.com.gestaoacoes.controller;

import br.com.gestaoacoes.dto.CorretoraRequest;
import br.com.gestaoacoes.dto.CorretoraResponse;
import br.com.gestaoacoes.mapper.CorretoraMapper;
import br.com.gestaoacoes.model.Corretora;
import br.com.gestaoacoes.service.CorretoraService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/corretoras")
public class CorretoraController {

    private final CorretoraService service;
    private final CorretoraMapper mapper;

    public CorretoraController(CorretoraService service, CorretoraMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CorretoraResponse registrar(@Valid @RequestBody CorretoraRequest request) {
        Corretora corretora = service.registrar(request);
        return mapper.toResponse(corretora);
    }

    @GetMapping
    public Page<CorretoraResponse> listar(Pageable pageable) {
        return service.listar(pageable).map(mapper::toResponse);
    }

    @GetMapping("/{id}")
    public CorretoraResponse buscarPorId(@PathVariable Long id) {
        return mapper.toResponse(service.buscarPorId(id));
    }

    @GetMapping("/cnpj/{cnpj}")
    public CorretoraResponse buscarPorCnpj(@PathVariable String cnpj) {
        return mapper.toResponse(service.buscarPorCnpj(cnpj));
    }
}