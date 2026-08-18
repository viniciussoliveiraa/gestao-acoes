package br.com.gestaoacoes.service;

import br.com.gestaoacoes.dto.LoginRequest;
import br.com.gestaoacoes.dto.RegistrarUsuarioRequest;
import br.com.gestaoacoes.exception.CredenciaisInvalidasException;
import br.com.gestaoacoes.exception.EmailJaCadastradoException;
import br.com.gestaoacoes.model.Usuario;
import br.com.gestaoacoes.repository.UsuarioRepository;
import br.com.gestaoacoes.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository repository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtService jwtService = new JwtService("segredo-de-teste-com-pelo-menos-32-caracteres-1234", 120);

    private AuthService service() {
        return new AuthService(repository, passwordEncoder, jwtService);
    }

    @Test
    void registrarComSucessoPersisteUsuarioComSenhaEmHash() {
        AuthService service = service();
        RegistrarUsuarioRequest request = new RegistrarUsuarioRequest("Ana", "Ana@Exemplo.com ", "senhaSegura123");
        when(repository.findByEmail("ana@exemplo.com")).thenReturn(Optional.empty());
        when(repository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Usuario usuario = service.registrar(request);

        assertThat(usuario.getEmail()).isEqualTo("ana@exemplo.com");
        assertThat(usuario.getSenhaHash()).isNotEqualTo("senhaSegura123");
        assertThat(passwordEncoder.matches("senhaSegura123", usuario.getSenhaHash())).isTrue();
    }

    @Test
    void registrarComEmailDuplicadoLancaExcecao() {
        AuthService service = service();
        RegistrarUsuarioRequest request = new RegistrarUsuarioRequest("Ana", "ana@exemplo.com", "senhaSegura123");
        when(repository.findByEmail("ana@exemplo.com"))
                .thenReturn(Optional.of(new Usuario("Ana", "ana@exemplo.com", "hash", OffsetDateTime.now())));

        assertThatThrownBy(() -> service.registrar(request)).isInstanceOf(EmailJaCadastradoException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void loginComCredenciaisValidasRetornaToken() {
        AuthService service = service();
        String senhaHash = passwordEncoder.encode("senhaSegura123");
        Usuario usuario = new Usuario("Ana", "ana@exemplo.com", senhaHash, OffsetDateTime.now());
        when(repository.findByEmail("ana@exemplo.com")).thenReturn(Optional.of(usuario));

        String token = service.login(new LoginRequest("ana@exemplo.com", "senhaSegura123"));

        assertThat(token).isNotBlank();
    }

    @Test
    void loginComSenhaIncorretaLancaCredenciaisInvalidas() {
        AuthService service = service();
        String senhaHash = passwordEncoder.encode("senhaSegura123");
        Usuario usuario = new Usuario("Ana", "ana@exemplo.com", senhaHash, OffsetDateTime.now());
        when(repository.findByEmail("ana@exemplo.com")).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.login(new LoginRequest("ana@exemplo.com", "senhaErrada")))
                .isInstanceOf(CredenciaisInvalidasException.class);
    }

    @Test
    void loginComEmailInexistenteLancaCredenciaisInvalidasComMensagemGenerica() {
        AuthService service = service();
        when(repository.findByEmail("ninguem@exemplo.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginRequest("ninguem@exemplo.com", "qualquer")))
                .isInstanceOf(CredenciaisInvalidasException.class)
                .hasMessage("Email ou senha inválidos");
    }
}