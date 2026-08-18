package br.com.gestaoacoes.repository;

import br.com.gestaoacoes.model.Acao;
import br.com.gestaoacoes.model.Mercado;
import br.com.gestaoacoes.model.Moeda;
import br.com.gestaoacoes.model.Provento;
import br.com.gestaoacoes.model.TipoProvento;
import br.com.gestaoacoes.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProventoRepositoryTest {

    @Autowired
    private ProventoRepository repository;
    @Autowired
    private AcaoRepository acaoRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    private Acao petr4;
    private Long usuario1Id;
    private Long usuario2Id;

    @BeforeEach
    void setUp() {
        petr4 = acaoRepository.save(new Acao("PETR4", "Petrobras", Mercado.BRASIL, Moeda.BRL,
                new BigDecimal("35.0000"), OffsetDateTime.now(), "teste", OffsetDateTime.now()));
        usuario1Id = usuarioRepository.save(new Usuario("Usuario 1", "u1@exemplo.com", "hash", OffsetDateTime.now())).getId();
        usuario2Id = usuarioRepository.save(new Usuario("Usuario 2", "u2@exemplo.com", "hash", OffsetDateTime.now())).getId();
    }

    @Test
    void listaProventosDoUsuarioOrdenadosPorDataPagamentoDesc() {
        repository.save(novoProvento(usuario1Id, LocalDate.of(2026, 1, 15)));
        repository.save(novoProvento(usuario1Id, LocalDate.of(2026, 6, 15)));
        repository.save(novoProvento(usuario2Id, LocalDate.of(2026, 3, 15)));

        Page<Provento> pagina = repository.findByUsuarioIdOrderByDataPagamentoDesc(usuario1Id, PageRequest.of(0, 10));

        assertThat(pagina.getTotalElements()).isEqualTo(2);
        assertThat(pagina.getContent().get(0).getDataPagamento()).isEqualTo(LocalDate.of(2026, 6, 15));
        assertThat(pagina.getContent().get(1).getDataPagamento()).isEqualTo(LocalDate.of(2026, 1, 15));
    }

    @Test
    void naoRetornaProventosDeOutroUsuario() {
        repository.save(novoProvento(usuario1Id, LocalDate.of(2026, 1, 15)));

        Page<Provento> pagina = repository.findByUsuarioIdOrderByDataPagamentoDesc(usuario2Id, PageRequest.of(0, 10));

        assertThat(pagina.getTotalElements()).isZero();
    }

    private Provento novoProvento(Long usuarioId, LocalDate dataPagamento) {
        return new Provento(usuarioId, petr4, TipoProvento.DIVIDENDO, new BigDecimal("45.9000"),
                dataPagamento, OffsetDateTime.now());
    }
}