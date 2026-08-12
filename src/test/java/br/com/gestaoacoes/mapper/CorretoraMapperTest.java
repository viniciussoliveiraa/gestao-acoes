package br.com.gestaoacoes.mapper;

import br.com.gestaoacoes.dto.CorretoraResponse;
import br.com.gestaoacoes.model.Corretora;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CorretoraMapperTest {

    private final CorretoraMapper mapper = new CorretoraMapper();

    @Test
    void mapeiaEntidadeParaResponse() {
        Corretora corretora = new Corretora(
                "11222333000181", "Razao LTDA", "Fantasia", "contato@corretora.com", "1130000000",
                "01310100", "Av. Paulista", "1000", "Sala 1", "Bela Vista", "Sao Paulo", "SP",
                "ATIVA", true, OffsetDateTime.parse("2026-08-12T10:00:00-03:00"));

        CorretoraResponse response = mapper.toResponse(corretora);

        assertThat(response.cnpj()).isEqualTo("11222333000181");
        assertThat(response.razaoSocial()).isEqualTo("Razao LTDA");
        assertThat(response.validadaCvm()).isTrue();
        assertThat(response.uf()).isEqualTo("SP");
    }
}