package br.com.gestaoacoes.config;

import br.com.gestaoacoes.model.Papel;
import br.com.gestaoacoes.model.Usuario;
import br.com.gestaoacoes.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminInitializerTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void criaAdminQuandoEmailAindaNaoExiste() {
        when(usuarioRepository.findByEmail("admin@exemplo.com")).thenReturn(Optional.empty());
        AdminInitializer initializer = new AdminInitializer(usuarioRepository, passwordEncoder,
                "Admin", "Admin@Exemplo.com", "senhaForte123");

        initializer.criarAdministradorInicial();

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario salvo = captor.getValue();
        assertThat(salvo.getEmail()).isEqualTo("admin@exemplo.com");
        assertThat(salvo.getPapel()).isEqualTo(Papel.ADMIN);
        assertThat(salvo.getAtivo()).isTrue();
        assertThat(passwordEncoder.matches("senhaForte123", salvo.getSenhaHash())).isTrue();
    }

    @Test
    void naoCriaAdminQuandoEmailJaExiste() {
        when(usuarioRepository.findByEmail("admin@exemplo.com"))
                .thenReturn(Optional.of(new Usuario("Admin", "admin@exemplo.com", "hash", Papel.ADMIN, true, OffsetDateTime.now())));
        AdminInitializer initializer = new AdminInitializer(usuarioRepository, passwordEncoder,
                "Admin", "admin@exemplo.com", "senhaForte123");

        initializer.criarAdministradorInicial();

        verify(usuarioRepository, never()).save(any());
    }
}
