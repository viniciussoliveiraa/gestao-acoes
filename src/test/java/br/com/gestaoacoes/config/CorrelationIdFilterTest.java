package br.com.gestaoacoes.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Sem mocks do Mockito: request, response e chain do spring-test são fakes leves e reais o
 * bastante para observar o comportamento público do filtro (cabeçalho devolvido e MDC).
 */
class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void reaproveitaOCorrelationIdRecebidoEOExpoeNoMdcDuranteARequisicao() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER, "abc-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> mdcDuranteARequisicao = new AtomicReference<>();

        filter.doFilter(request, response, new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                mdcDuranteARequisicao.set(MDC.get(CorrelationIdFilter.MDC_KEY));
            }
        });

        assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isEqualTo("abc-123");
        assertThat(mdcDuranteARequisicao.get()).isEqualTo("abc-123");
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void geraUmUuidQuandoOCabecalhoNaoEhEnviado() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest(), response, new MockFilterChain());

        String gerado = response.getHeader(CorrelationIdFilter.HEADER);
        assertThatCode(() -> UUID.fromString(gerado)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void geraUmUuidQuandoOCabecalhoVemEmBranco(String valor) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER, valor);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        String gerado = response.getHeader(CorrelationIdFilter.HEADER);
        assertThat(gerado).isNotBlank();
        assertThatCode(() -> UUID.fromString(gerado)).doesNotThrowAnyException();
    }
}
