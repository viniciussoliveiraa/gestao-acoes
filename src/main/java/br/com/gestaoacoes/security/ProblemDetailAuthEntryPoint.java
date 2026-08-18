package br.com.gestaoacoes.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;

/**
 * Traduz falhas de autenticação do Spring Security (401) para o mesmo formato Problem Details
 * (RFC 9457) usado pelo {@link br.com.gestaoacoes.exception.GlobalExceptionHandler} no restante
 * da API, em vez do corpo padrão do Spring Security.
 */
@Component
public class ProblemDetailAuthEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                          AuthenticationException authException) throws IOException {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED, "Autenticação necessária ou token inválido/expirado");
        problemDetail.setTitle(HttpStatus.UNAUTHORIZED.getReasonPhrase());
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setInstance(URI.create(request.getRequestURI()));

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        ProblemDetailJson.MAPPER.writeValue(response.getWriter(), problemDetail);
    }
}