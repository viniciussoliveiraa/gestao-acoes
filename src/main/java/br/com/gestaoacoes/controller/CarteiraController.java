package br.com.gestaoacoes.controller;

import br.com.gestaoacoes.dto.LancamentoRequest;
import br.com.gestaoacoes.dto.LancamentoResponse;
import br.com.gestaoacoes.dto.PosicaoResponse;
import br.com.gestaoacoes.mapper.LancamentoMapper;
import br.com.gestaoacoes.model.Lancamento;
import br.com.gestaoacoes.service.CarteiraService;
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

import java.util.List;

@RestController
@RequestMapping("/carteira")
public class CarteiraController {

    private final CarteiraService service;
    private final LancamentoMapper mapper;

    public CarteiraController(CarteiraService service, LancamentoMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping("/lancamentos")
    @ResponseStatus(HttpStatus.CREATED)
    public LancamentoResponse registrarLancamento(@AuthenticationPrincipal Long usuarioId,
                                                   @Valid @RequestBody LancamentoRequest request) {
        Lancamento lancamento = service.registrarLancamento(usuarioId, request);
        return mapper.toResponse(lancamento);
    }

    @GetMapping("/lancamentos")
    public Page<LancamentoResponse> listarLancamentos(@AuthenticationPrincipal Long usuarioId, Pageable pageable) {
        return service.listarLancamentos(usuarioId, pageable).map(mapper::toResponse);
    }

    @GetMapping("/posicoes")
    public List<PosicaoResponse> listarPosicoes(@AuthenticationPrincipal Long usuarioId) {
        return service.listarPosicoes(usuarioId);
    }
}