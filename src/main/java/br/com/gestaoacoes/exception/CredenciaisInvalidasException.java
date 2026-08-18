package br.com.gestaoacoes.exception;

import org.springframework.http.HttpStatus;

public class CredenciaisInvalidasException extends ApiException {

    public CredenciaisInvalidasException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}