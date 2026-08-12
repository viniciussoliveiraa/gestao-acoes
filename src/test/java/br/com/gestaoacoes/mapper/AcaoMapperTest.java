package br.com.gestaoacoes.mapper;

import br.com.gestaoacoes.dto.AcaoResponse;
import br.com.gestaoacoes.model.Acao;
import br.com.gestaoacoes.model.Mercado;
import br.com.gestaoacoes.model.Moeda;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AcaoMapperTest {

    private final AcaoMapper mapper = new AcaoMapper();

    @Test
    void mapeiaEntidadeParaResponse() {
        Acao acao = new Acao(
                "PETR4", "Petrobras", Mercado.BRASIL, Moeda.BRL, new BigDecimal("38.4200"),
                OffsetDateTime.parse("2026-08-12T10:00:00-03:00"), "brapi", OffsetDateTime.parse("2026-08-12T10:00:00-03:00"));

        AcaoResponse response = mapper.toResponse(acao);

        assertThat(response.ticker()).isEqualTo("PETR4");
        assertThat(response.mercado()).isEqualTo(Mercado.BRASIL);
        assertThat(response.cotacaoAtual()).isEqualByComparingTo("38.4200");
    }
}