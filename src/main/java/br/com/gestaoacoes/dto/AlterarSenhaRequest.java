package br.com.gestaoacoes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AlterarSenhaRequest(
        @NotBlank(message = "senha é obrigatória") @Size(min = 8, message = "senha deve ter ao menos 8 caracteres") String senha
) {
}
