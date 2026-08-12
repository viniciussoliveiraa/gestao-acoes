package br.com.gestaoacoes.exception;

import org.springframework.http.HttpStatus;

public class IntegracaoExternaIndisponivelException extends ApiException {

    public IntegracaoExternaIndisponivelException(String message) {
        super(HttpStatus.BAD_GATEWAY, message);
    }

    public IntegracaoExternaIndisponivelException(String message, Throwable cause) {
        super(HttpStatus.BAD_GATEWAY, message);
        initCause(cause);
    }
}