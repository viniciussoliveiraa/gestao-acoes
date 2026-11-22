package br.com.gestaoacoes.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Caminhos do fallback genérico que a suíte de controllers não alcança: uma exceção inesperada
 * não pode vazar detalhes internos, e exceções do Spring MVC que já trazem status próprio não
 * podem ser mascaradas como 500.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void excecaoInesperadaViraErro500ComMensagemGenericaSemVazarDetalhes() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/carteira/posicoes");

        ProblemDetail problema = handler.handleUnexpected(new IllegalStateException("senha do banco: xyz"), request);

        assertThat(problema.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problema.getDetail()).isEqualTo("Erro interno inesperado").doesNotContain("xyz");
        assertThat(problema.getInstance()).isEqualTo(URI.create("/carteira/posicoes"));
    }

    @Test
    void excecaoDoSpringComStatusProprioPreservaOStatusEAInstanciaJaDefinida() {
        HttpRequestMethodNotSupportedException ex =
                new HttpRequestMethodNotSupportedException("DELETE", List.of(HttpMethod.GET.name()));
        ex.getBody().setInstance(URI.create("/original"));
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/acoes");

        ProblemDetail problema = handler.handleUnexpected(ex, request);

        assertThat(problema.getStatus()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED.value());
        assertThat(problema.getInstance()).isEqualTo(URI.create("/original"));
        assertThat(problema.getProperties()).containsKey("timestamp");
    }

    @Test
    void excecaoDoSpringSemInstanciaRecebeAUriDaRequisicao() {
        HttpRequestMethodNotSupportedException ex =
                new HttpRequestMethodNotSupportedException("DELETE", List.of(HttpMethod.GET.name()));
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/acoes");

        ProblemDetail problema = handler.handleUnexpected(ex, request);

        assertThat(problema.getStatus()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED.value());
        assertThat(problema.getInstance()).isEqualTo(URI.create("/acoes"));
    }
}
