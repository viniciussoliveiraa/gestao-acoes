package br.com.gestaoacoes.dto;

import br.com.gestaoacoes.model.TipoProvento;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProventoRequest(
        @NotNull(message = "acaoId é obrigatório") Long acaoId,
        @NotNull(message = "tipo é obrigatório") TipoProvento tipo,
        @NotNull(message = "valorTotal é obrigatório") @Positive(message = "valorTotal deve ser maior que zero") BigDecimal valorTotal,
        @NotNull(message = "dataPagamento é obrigatória") LocalDate dataPagamento
) {
}