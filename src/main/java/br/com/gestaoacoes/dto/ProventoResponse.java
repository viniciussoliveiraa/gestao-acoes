package br.com.gestaoacoes.dto;

import br.com.gestaoacoes.model.TipoProvento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record ProventoResponse(
        Long id,
        Long acaoId,
        String tickerAcao,
        TipoProvento tipo,
        BigDecimal valorTotal,
        LocalDate dataPagamento,
        OffsetDateTime criadoEm
) {
}