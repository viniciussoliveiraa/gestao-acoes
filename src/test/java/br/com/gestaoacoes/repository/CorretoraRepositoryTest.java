package br.com.gestaoacoes.repository;

import br.com.gestaoacoes.model.Corretora;
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
class CorretoraRepositoryTest {

    @Autowired
    private CorretoraRepository repository;

    @Test
    void salvaEBuscaPorCnpj() {
        repository.save(novaCorretora("11222333000181"));

        Optional<Corretora> encontrada = repository.findByCnpj("11222333000181");

        assertThat(encontrada).isPresent();
        assertThat(encontrada.get().getRazaoSocial()).isEqualTo("Razao LTDA");
    }

    @Test
    void rejeitaCnpjDuplicado() {
        repository.saveAndFlush(novaCorretora("11222333000181"));

        assertThatThrownBy(() -> repository.saveAndFlush(novaCorretora("11222333000181")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Corretora novaCorretora(String cnpj) {
        return new Corretora(cnpj, "Razao LTDA", "Fantasia", "contato@corretora.com", "1130000000",
                "01310100", "Av. Paulista", "1000", null, "Bela Vista", "Sao Paulo", "SP",
                "ATIVA", true, OffsetDateTime.now());
    }
}