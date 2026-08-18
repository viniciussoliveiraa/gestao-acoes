package br.com.gestaoacoes.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;

/**
 * Traduz falhas de autorização do Spring Security (403) para o mesmo formato Problem Details
 * usado no restante da API.
 */
@Component
public class ProblemDetailAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                        AccessDeniedException accessDeniedException) throws IOException {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN, "Acesso negado ao recurso solicitado");
        problemDetail.setTitle(HttpStatus.FORBIDDEN.getReasonPhrase());
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setInstance(URI.create(request.getRequestURI()));

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        ProblemDetailJson.MAPPER.writeValue(response.getWriter(), problemDetail);
    }
}