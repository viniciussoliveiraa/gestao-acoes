package br.com.gestaoacoes.exception;

import org.springframework.http.HttpStatus;

public class CnpjNaoEncontradoException extends ApiException {

    public CnpjNaoEncontradoException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}