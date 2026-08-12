package br.com.gestaoacoes.exception;

import org.springframework.http.HttpStatus;

public class TickerAmbiguoException extends ApiException {

    public TickerAmbiguoException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}