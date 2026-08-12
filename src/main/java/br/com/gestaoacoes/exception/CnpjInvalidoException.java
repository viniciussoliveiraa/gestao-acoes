package br.com.gestaoacoes.exception;

import org.springframework.http.HttpStatus;

public class CnpjInvalidoException extends ApiException {

    public CnpjInvalidoException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}