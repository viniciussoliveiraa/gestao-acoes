package br.com.gestaoacoes.dto;

import br.com.gestaoacoes.model.Mercado;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AcaoRequest(
        @NotBlank(message = "ticker é obrigatório") String ticker,
        @NotNull(message = "mercado é obrigatório") Mercado mercado
) {
}