package br.com.gestaoacoes.exception;

import org.springframework.http.HttpStatus;

public class EmailJaCadastradoException extends ApiException {

    public EmailJaCadastradoException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}