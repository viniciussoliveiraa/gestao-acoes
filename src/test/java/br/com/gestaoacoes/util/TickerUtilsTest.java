package br.com.gestaoacoes.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TickerUtilsTest {

    @Test
    void normalizarConvertePraMaiusculoERemoveEspacos() {
        assertThat(TickerUtils.normalizar(" petr4 ")).isEqualTo("PETR4");
    }

    @Test
    void normalizarLidaComNulo() {
        assertThat(TickerUtils.normalizar(null)).isEmpty();
    }
}