package br.com.gestaoacoes.dto;

import br.com.gestaoacoes.model.Papel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Cadastro de usuário pelo administrador (Aula 14, {@code POST /usuarios}), que define o papel
 * explicitamente — diferente de {@code POST /auth/registrar} (autocadastro público, sempre
 * {@code USER}).
 */
public record UsuarioCreateRequest(
        @NotBlank(message = "nome é obrigatório") String nome,
        @NotBlank(message = "email é obrigatório") @Email(message = "email inválido") String email,
        @NotBlank(message = "senha é obrigatória") @Size(min = 8, message = "senha deve ter ao menos 8 caracteres") String senha,
        @NotNull(message = "papel é obrigatório") Papel papel
) {
}
