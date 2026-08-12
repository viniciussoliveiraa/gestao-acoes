package br.com.gestaoacoes.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CnpjUtilsTest {

    @Test
    void normalizarRemoveMascara() {
        assertThat(CnpjUtils.normalizar("11.222.333/0001-81")).isEqualTo("11222333000181");
    }

    @Test
    void isValidoAceitaCnpjComDigitosVerificadoresCorretos() {
        assertThat(CnpjUtils.isValido("11222333000181")).isTrue();
    }

    @Test
    void isValidoRejeitaDigitosVerificadoresIncorretos() {
        assertThat(CnpjUtils.isValido("11222333000180")).isFalse();
    }

    @Test
    void isValidoRejeitaTamanhoDiferenteDe14() {
        assertThat(CnpjUtils.isValido("1122233300018")).isFalse();
    }

    @Test
    void isValidoRejeitaTodosDigitosIguais() {
        assertThat(CnpjUtils.isValido("11111111111111")).isFalse();
    }

    @Test
    void isValidoRejeitaNulo() {
        assertThat(CnpjUtils.isValido(null)).isFalse();
    }
}