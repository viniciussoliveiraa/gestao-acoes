package br.com.gestaoacoes.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ProblemDetail handleApiException(ApiException ex, HttpServletRequest request) {
        return build(ex.getStatus(), ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<Map<String, String>> erros = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .toList();
        ProblemDetail problemDetail = build(HttpStatus.BAD_REQUEST, "Um ou mais campos são inválidos", request);
        problemDetail.setProperty("errors", erros);
        return problemDetail;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "Registro duplicado: viola uma restrição de unicidade", request);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno inesperado", request);
    }

    private ProblemDetail build(HttpStatus status, String detail, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(status.getReasonPhrase());
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setInstance(java.net.URI.create(request.getRequestURI()));
        return problemDetail;
    }

    private Map<String, String> toFieldError(FieldError fieldError) {
        return Map.of("campo", fieldError.getField(), "mensagem", String.valueOf(fieldError.getDefaultMessage()));
    }
}