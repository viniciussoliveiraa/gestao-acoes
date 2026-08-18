package br.com.gestaoacoes.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record LancamentoResponse(
        Long id,
        Long acaoId,
        String tickerAcao,
        Long corretoraId,
        String razaoSocialCorretora,
        BigDecimal quantidade,
        BigDecimal precoUnitario,
        LocalDate dataOperacao,
        OffsetDateTime criadoEm
) {
}