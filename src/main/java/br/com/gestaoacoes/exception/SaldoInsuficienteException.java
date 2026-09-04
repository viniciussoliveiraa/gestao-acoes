package br.com.gestaoacoes.exception;

import org.springframework.http.HttpStatus;

public class SaldoInsuficienteException extends ApiException {

    public SaldoInsuficienteException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }
}
