package br.com.gestaoacoes.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrarUsuarioRequest(
        @NotBlank(message = "nome é obrigatório") String nome,
        @NotBlank(message = "email é obrigatório") @Email(message = "email inválido") String email,
        @NotBlank(message = "senha é obrigatória") @Size(min = 8, max = 72, message = "senha deve ter entre 8 e 72 caracteres") String senha
) {
}