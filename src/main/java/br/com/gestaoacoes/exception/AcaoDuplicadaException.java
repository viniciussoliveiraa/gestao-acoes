package br.com.gestaoacoes.exception;

import org.springframework.http.HttpStatus;

public class AcaoDuplicadaException extends ApiException {

    public AcaoDuplicadaException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}