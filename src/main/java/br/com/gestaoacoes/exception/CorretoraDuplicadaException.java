package br.com.gestaoacoes.exception;

import org.springframework.http.HttpStatus;

public class CorretoraDuplicadaException extends ApiException {

    public CorretoraDuplicadaException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}