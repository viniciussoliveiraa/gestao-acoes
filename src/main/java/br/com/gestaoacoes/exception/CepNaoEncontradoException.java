package br.com.gestaoacoes.exception;

import org.springframework.http.HttpStatus;

public class CepNaoEncontradoException extends ApiException {

    public CepNaoEncontradoException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}