package br.com.gestaoacoes.exception;

import org.springframework.http.HttpStatus;

/**
 * Impede que o sistema fique sem nenhum administrador ativo (Aula 14, seção 12.1): rebaixar ou
 * desativar o último {@code ADMIN} ativo trancaria para sempre o gerenciamento de usuários.
 */
public class UltimoAdministradorException extends ApiException {

    public UltimoAdministradorException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }
}
