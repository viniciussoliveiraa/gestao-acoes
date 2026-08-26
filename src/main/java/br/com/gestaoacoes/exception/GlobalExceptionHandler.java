package br.com.gestaoacoes.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
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

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleMessageNotReadable(HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido ou malformado", request);
    }

    /**
     * Fallback genérico. Algumas exceções internas do Spring MVC que chegam aqui (rota
     * inexistente, método HTTP não suportado, etc.) já implementam {@link ErrorResponse} e
     * carregam o status/corpo corretos — nesse caso reaproveitamos o {@code ProblemDetail}
     * delas em vez de mascarar tudo como 500.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        if (ex instanceof ErrorResponse errorResponse) {
            ProblemDetail problemDetail = errorResponse.getBody();
            problemDetail.setProperty("timestamp", Instant.now());
            if (problemDetail.getInstance() == null) {
                problemDetail.setInstance(java.net.URI.create(request.getRequestURI()));
            }
            return problemDetail;
        }
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