package br.com.gestaoacoes.exception;

import org.springframework.http.HttpStatus;

public class CepInvalidoException extends ApiException {

    public CepInvalidoException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}