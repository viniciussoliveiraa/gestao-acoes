package br.com.gestaoacoes.dto;

import br.com.gestaoacoes.model.Papel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Alteração cadastral de um usuário pelo administrador (Aula 14, {@code PUT /usuarios/{id}}).
 * Não recebe senha — trocar senha é uma operação separada ({@link AlterarSenhaRequest}), para
 * reduzir o risco de alterar uma credencial por engano junto com dados cadastrais.
 */
public record UsuarioUpdateRequest(
        @NotBlank(message = "nome é obrigatório") String nome,
        @NotBlank(message = "email é obrigatório") @Email(message = "email inválido") String email,
        @NotNull(message = "papel é obrigatório") Papel papel,
        @NotNull(message = "ativo é obrigatório") Boolean ativo
) {
}
