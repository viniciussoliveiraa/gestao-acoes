package br.com.gestaoacoes.service;

import br.com.gestaoacoes.dto.LoginRequest;
import br.com.gestaoacoes.dto.RegistrarUsuarioRequest;
import br.com.gestaoacoes.exception.CredenciaisInvalidasException;
import br.com.gestaoacoes.exception.EmailJaCadastradoException;
import br.com.gestaoacoes.model.Usuario;
import br.com.gestaoacoes.repository.UsuarioRepository;
import br.com.gestaoacoes.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class AuthService {

    private static final String MENSAGEM_CREDENCIAIS_INVALIDAS = "Email ou senha inválidos";

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UsuarioRepository repository, PasswordEncoder passwordEncoder, JwtService jwtService,
                        AuthenticationManager authenticationManager) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    public Usuario registrar(RegistrarUsuarioRequest request) {
        String email = normalizarEmail(request.email());

        if (repository.findByEmail(email).isPresent()) {
            throw new EmailJaCadastradoException("Já existe um usuário cadastrado com este email");
        }

        // Autocadastro público: sempre USER (veja Usuario(nome, email, senhaHash, criadoEm)).
        // Um ADMIN é promovido depois via /usuarios, ou nasce do bootstrap do administrador inicial.
        Usuario usuario = new Usuario(
                request.nome().trim(),
                email,
                passwordEncoder.encode(request.senha()),
                OffsetDateTime.now()
        );

        return repository.save(usuario);
    }

    /**
     * Autentica via {@link AuthenticationManager} (que delega ao {@code DaoAuthenticationProvider}
     * configurado automaticamente a partir de {@code UsuarioDetailsService} + {@code PasswordEncoder})
     * e, se as credenciais forem válidas, emite um JWT com as authorities do usuário (Aula 14:
     * {@code ROLE_ADMIN} ou {@code ROLE_USER}), usadas depois por {@code @PreAuthorize}.
     *
     * <p>Credenciais inválidas e usuário desativado (ver {@code UsuarioDetailsService#disabled})
     * recebem a mesma mensagem genérica — o sistema nunca indica qual dos dois foi o motivo.
     */
    public String login(LoginRequest request) {
        String email = normalizarEmail(request.email());

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.senha()));
        } catch (AuthenticationException ex) {
            throw new CredenciaisInvalidasException(MENSAGEM_CREDENCIAIS_INVALIDAS);
        }

        Usuario usuario = repository.findByEmail(email)
                .orElseThrow(() -> new CredenciaisInvalidasException(MENSAGEM_CREDENCIAIS_INVALIDAS));

        List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return jwtService.gerarToken(usuario.getId(), usuario.getEmail(), authorities);
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase();
    }
}
