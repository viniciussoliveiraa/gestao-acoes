package br.com.gestaoacoes.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CorretoraRequest(
        @NotBlank(message = "cnpj é obrigatório") String cnpj,
        @NotBlank(message = "cep é obrigatório") String cep,
        String numero,
        String complemento,
        @Email(message = "email inválido") String email,
        String telefone
) {
}