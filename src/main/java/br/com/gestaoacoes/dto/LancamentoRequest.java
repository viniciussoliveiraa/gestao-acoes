package br.com.gestaoacoes.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LancamentoRequest(
        @NotNull(message = "acaoId é obrigatório") Long acaoId,
        @NotNull(message = "corretoraId é obrigatório") Long corretoraId,
        @NotNull(message = "quantidade é obrigatória") @Positive(message = "quantidade deve ser maior que zero") BigDecimal quantidade,
        @NotNull(message = "precoUnitario é obrigatório") @Positive(message = "precoUnitario deve ser maior que zero") BigDecimal precoUnitario,
        @NotNull(message = "dataOperacao é obrigatória") LocalDate dataOperacao
) {
}