package br.com.gestaoacoes.dto;

import br.com.gestaoacoes.model.TipoLancamento;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LancamentoRequest(
        @NotNull(message = "acaoId é obrigatório") Long acaoId,
        @NotNull(message = "corretoraId é obrigatório") Long corretoraId,
        // Omitido = COMPRA, para manter compatibilidade com clientes anteriores a venda existir.
        TipoLancamento tipo,
        @NotNull(message = "quantidade é obrigatória") @Positive(message = "quantidade deve ser maior que zero") BigDecimal quantidade,
        @NotNull(message = "precoUnitario é obrigatório") @Positive(message = "precoUnitario deve ser maior que zero") BigDecimal precoUnitario,
        @NotNull(message = "dataOperacao é obrigatória") LocalDate dataOperacao
) {
    public TipoLancamento tipoOuPadrao() {
        return tipo != null ? tipo : TipoLancamento.COMPRA;
    }
}