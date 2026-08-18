package br.com.gestaoacoes.controller;

import br.com.gestaoacoes.dto.LoginRequest;
import br.com.gestaoacoes.dto.LoginResponse;
import br.com.gestaoacoes.dto.RegistrarUsuarioRequest;
import br.com.gestaoacoes.dto.UsuarioResponse;
import br.com.gestaoacoes.mapper.UsuarioMapper;
import br.com.gestaoacoes.model.Usuario;
import br.com.gestaoacoes.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService service;
    private final UsuarioMapper mapper;

    public AuthController(AuthService service, UsuarioMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping("/registrar")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse registrar(@Valid @RequestBody RegistrarUsuarioRequest request) {
        Usuario usuario = service.registrar(request);
        return mapper.toResponse(usuario);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        String token = service.login(request);
        return new LoginResponse(token);
    }
}