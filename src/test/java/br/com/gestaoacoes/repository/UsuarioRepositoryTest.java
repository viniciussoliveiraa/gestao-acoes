package br.com.gestaoacoes.repository;

import br.com.gestaoacoes.model.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UsuarioRepositoryTest {

    @Autowired
    private UsuarioRepository repository;

    @Test
    void salvaEBuscaPorEmail() {
        repository.save(novoUsuario("ana@exemplo.com"));

        Optional<Usuario> encontrado = repository.findByEmail("ana@exemplo.com");

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getNome()).isEqualTo("Ana");
    }

    @Test
    void rejeitaEmailDuplicado() {
        repository.saveAndFlush(novoUsuario("ana@exemplo.com"));

        assertThatThrownBy(() -> repository.saveAndFlush(novoUsuario("ana@exemplo.com")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Usuario novoUsuario(String email) {
        return new Usuario("Ana", email, "hash-de-senha", OffsetDateTime.now());
    }
}