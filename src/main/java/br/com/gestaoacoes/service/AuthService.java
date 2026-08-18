package br.com.gestaoacoes.service;

import br.com.gestaoacoes.dto.LoginRequest;
import br.com.gestaoacoes.dto.RegistrarUsuarioRequest;
import br.com.gestaoacoes.exception.CredenciaisInvalidasException;
import br.com.gestaoacoes.exception.EmailJaCadastradoException;
import br.com.gestaoacoes.model.Usuario;
import br.com.gestaoacoes.repository.UsuarioRepository;
import br.com.gestaoacoes.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class AuthService {

    private static final String MENSAGEM_CREDENCIAIS_INVALIDAS = "Email ou senha inválidos";

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository repository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public Usuario registrar(RegistrarUsuarioRequest request) {
        String email = normalizarEmail(request.email());

        if (repository.findByEmail(email).isPresent()) {
            throw new EmailJaCadastradoException("Já existe um usuário cadastrado com este email");
        }

        Usuario usuario = new Usuario(
                request.nome().trim(),
                email,
                passwordEncoder.encode(request.senha()),
                OffsetDateTime.now()
        );

        return repository.save(usuario);
    }

    public String login(LoginRequest request) {
        String email = normalizarEmail(request.email());

        Usuario usuario = repository.findByEmail(email)
                .orElseThrow(() -> new CredenciaisInvalidasException(MENSAGEM_CREDENCIAIS_INVALIDAS));

        if (!passwordEncoder.matches(request.senha(), usuario.getSenhaHash())) {
            throw new CredenciaisInvalidasException(MENSAGEM_CREDENCIAIS_INVALIDAS);
        }

        return jwtService.gerarToken(usuario.getId(), usuario.getEmail());
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase();
    }
}