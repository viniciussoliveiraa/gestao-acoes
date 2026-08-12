package br.com.gestaoacoes.repository;

import br.com.gestaoacoes.model.Acao;
import br.com.gestaoacoes.model.Mercado;
import br.com.gestaoacoes.model.Moeda;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AcaoRepositoryTest {

    @Autowired
    private AcaoRepository repository;

    @Test
    void salvaEBuscaPorTickerEMercado() {
        repository.save(novaAcao("PETR4", Mercado.BRASIL));

        Optional<Acao> encontrada = repository.findByTickerAndMercado("PETR4", Mercado.BRASIL);

        assertThat(encontrada).isPresent();
        assertThat(encontrada.get().getMoeda()).isEqualTo(Moeda.BRL);
    }

    @Test
    void rejeitaMesmoTickerEMercadoDuplicado() {
        repository.saveAndFlush(novaAcao("PETR4", Mercado.BRASIL));

        assertThatThrownBy(() -> repository.saveAndFlush(novaAcao("PETR4", Mercado.BRASIL)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void permiteMesmoTickerEmMercadosDiferentes() {
        repository.saveAndFlush(novaAcao("IBM", Mercado.ESTADOS_UNIDOS));

        repository.saveAndFlush(novaAcao("IBM", Mercado.BRASIL));

        assertThat(repository.findByTicker("IBM")).hasSize(2);
    }

    private Acao novaAcao(String ticker, Mercado mercado) {
        Moeda moeda = mercado == Mercado.BRASIL ? Moeda.BRL : Moeda.USD;
        return new Acao(ticker, "Empresa Teste", mercado, moeda, new BigDecimal("10.0000"),
                OffsetDateTime.now(), "teste", OffsetDateTime.now());
    }
}