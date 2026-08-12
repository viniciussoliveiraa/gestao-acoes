package br.com.gestaoacoes.exception;

import org.springframework.http.HttpStatus;

public class InstituicaoNaoValidadaException extends ApiException {

    public InstituicaoNaoValidadaException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }
}