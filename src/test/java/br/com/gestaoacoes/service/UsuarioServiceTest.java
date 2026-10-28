package br.com.gestaoacoes.service;

import br.com.gestaoacoes.dto.AlterarSenhaRequest;
import br.com.gestaoacoes.dto.UsuarioCreateRequest;
import br.com.gestaoacoes.dto.UsuarioUpdateRequest;
import br.com.gestaoacoes.exception.EmailJaCadastradoException;
import br.com.gestaoacoes.exception.RecursoNaoEncontradoException;
import br.com.gestaoacoes.model.Papel;
import br.com.gestaoacoes.model.Usuario;
import br.com.gestaoacoes.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository repository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private UsuarioService service() {
        return new UsuarioService(repository, passwordEncoder);
    }

    private Usuario usuarioExistente(Long id, String email, Papel papel) {
        Usuario usuario = new Usuario("Nome", email, "hash", papel, true, OffsetDateTime.now());
        ReflectionTestUtils.setField(usuario, "id", id);
        return usuario;
    }

    @Test
    void listarDelegaAoRepository() {
        Pageable pageable = Pageable.ofSize(10);
        Page<Usuario> pagina = new PageImpl<>(List.of(usuarioExistente(1L, "a@exemplo.com", Papel.USER)));
        when(repository.findAll(pageable)).thenReturn(pagina);

        assertThat(service().listar(pageable)).isEqualTo(pagina);
    }

    @Test
    void buscarPorIdInexistenteLancaRecursoNaoEncontrado() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().buscarPorId(99L)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void criarComSucessoCodificaSenhaEUsaPapelInformado() {
        when(repository.findByEmail("novo@exemplo.com")).thenReturn(Optional.empty());
        when(repository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Usuario usuario = service().criar(new UsuarioCreateRequest("Novo", "Novo@Exemplo.com ", "senhaSegura123", Papel.ADMIN));

        assertThat(usuario.getEmail()).isEqualTo("novo@exemplo.com");
        assertThat(usuario.getPapel()).isEqualTo(Papel.ADMIN);
        assertThat(usuario.getAtivo()).isTrue();
        assertThat(passwordEncoder.matches("senhaSegura123", usuario.getSenhaHash())).isTrue();
    }

    @Test
    void criarComEmailJaCadastradoLancaExcecao() {
        when(repository.findByEmail("existente@exemplo.com"))
                .thenReturn(Optional.of(usuarioExistente(1L, "existente@exemplo.com", Papel.USER)));

        assertThatThrownBy(() -> service().criar(new UsuarioCreateRequest("Novo", "existente@exemplo.com", "senhaSegura123", Papel.USER)))
                .isInstanceOf(EmailJaCadastradoException.class);
    }

    @Test
    void atualizarComSucessoAlteraEmailPapelEAtivo() {
        Usuario existente = usuarioExistente(1L, "antigo@exemplo.com", Papel.USER);
        when(repository.findById(1L)).thenReturn(Optional.of(existente));
        when(repository.findByEmail("novo@exemplo.com")).thenReturn(Optional.empty());
        when(repository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Usuario atualizado = service().atualizar(1L, new UsuarioUpdateRequest("Nome", "novo@exemplo.com", Papel.ADMIN, false));

        assertThat(atualizado.getEmail()).isEqualTo("novo@exemplo.com");
        assertThat(atualizado.getPapel()).isEqualTo(Papel.ADMIN);
        assertThat(atualizado.getAtivo()).isFalse();
    }

    @Test
    void atualizarMantendoOProprioEmailNaoLancaConflito() {
        Usuario existente = usuarioExistente(1L, "mesmo@exemplo.com", Papel.USER);
        when(repository.findById(1L)).thenReturn(Optional.of(existente));
        when(repository.findByEmail("mesmo@exemplo.com")).thenReturn(Optional.of(existente));
        when(repository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Usuario atualizado = service().atualizar(1L, new UsuarioUpdateRequest("Nome", "mesmo@exemplo.com", Papel.USER, true));

        assertThat(atualizado.getEmail()).isEqualTo("mesmo@exemplo.com");
    }

    @Test
    void atualizarComEmailDeOutroUsuarioLancaConflito() {
        Usuario existente = usuarioExistente(1L, "a@exemplo.com", Papel.USER);
        Usuario outro = usuarioExistente(2L, "b@exemplo.com", Papel.USER);
        when(repository.findById(1L)).thenReturn(Optional.of(existente));
        when(repository.findByEmail("b@exemplo.com")).thenReturn(Optional.of(outro));

        assertThatThrownBy(() -> service().atualizar(1L, new UsuarioUpdateRequest("Nome", "b@exemplo.com", Papel.USER, true)))
                .isInstanceOf(EmailJaCadastradoException.class);
    }

    @Test
    void alterarSenhaGeraNovoHash() {
        Usuario existente = usuarioExistente(1L, "a@exemplo.com", Papel.USER);
        when(repository.findById(1L)).thenReturn(Optional.of(existente));
        when(repository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service().alterarSenha(1L, new AlterarSenhaRequest("novaSenhaForte123"));

        assertThat(passwordEncoder.matches("novaSenhaForte123", existente.getSenhaHash())).isTrue();
    }
}
