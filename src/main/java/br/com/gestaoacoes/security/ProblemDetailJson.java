package br.com.gestaoacoes.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.json.ProblemDetailJacksonMixin;

/**
 * {@link ObjectMapper} compartilhado por {@link ProblemDetailAuthEntryPoint} e
 * {@link ProblemDetailAccessDeniedHandler}, que rodam fora do pipeline MVC (filtros de
 * segurança) e por isso não recebem o {@code JsonMapper} gerenciado pelo Spring Boot.
 */
final class ProblemDetailJson {

    static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .addMixIn(ProblemDetail.class, ProblemDetailJacksonMixin.class);

    private ProblemDetailJson() {
    }
}