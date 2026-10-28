package br.com.gestaoacoes.controller;

import br.com.gestaoacoes.dto.AlterarSenhaRequest;
import br.com.gestaoacoes.dto.UsuarioCreateRequest;
import br.com.gestaoacoes.dto.UsuarioResponse;
import br.com.gestaoacoes.dto.UsuarioUpdateRequest;
import br.com.gestaoacoes.mapper.UsuarioMapper;
import br.com.gestaoacoes.model.Usuario;
import br.com.gestaoacoes.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gerenciamento administrativo de usuários (Aula 14). Toda a classe exige o papel {@code ADMIN} —
 * diferente de {@code /auth/registrar}/{@code /auth/login}, que continuam públicos.
 *
 * <p>Não há {@code DELETE}: para identidades, desativar ({@code ativo=false} via
 * {@code PUT /usuarios/{id}}) é preferível a remover, pois preserva o registro para auditoria e
 * impede novos logins sem apagar o histórico de quem fez o quê.
 */
@RestController
@RequestMapping("/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioController {

    private final UsuarioService service;
    private final UsuarioMapper mapper;

    public UsuarioController(UsuarioService service, UsuarioMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public Page<UsuarioResponse> listar(Pageable pageable) {
        return service.listar(pageable).map(mapper::toResponse);
    }

    @GetMapping("/{id}")
    public UsuarioResponse buscarPorId(@PathVariable Long id) {
        return mapper.toResponse(service.buscarPorId(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse criar(@Valid @RequestBody UsuarioCreateRequest request) {
        Usuario usuario = service.criar(request);
        return mapper.toResponse(usuario);
    }

    @PutMapping("/{id}")
    public UsuarioResponse atualizar(@PathVariable Long id, @Valid @RequestBody UsuarioUpdateRequest request) {
        Usuario usuario = service.atualizar(id, request);
        return mapper.toResponse(usuario);
    }

    @PutMapping("/{id}/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void alterarSenha(@PathVariable Long id, @Valid @RequestBody AlterarSenhaRequest request) {
        service.alterarSenha(id, request);
    }
}
