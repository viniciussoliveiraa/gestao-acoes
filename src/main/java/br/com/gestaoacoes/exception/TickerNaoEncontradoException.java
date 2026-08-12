package br.com.gestaoacoes.exception;

import org.springframework.http.HttpStatus;

public class TickerNaoEncontradoException extends ApiException {

    public TickerNaoEncontradoException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}