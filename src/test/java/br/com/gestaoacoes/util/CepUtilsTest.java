package br.com.gestaoacoes.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CepUtilsTest {

    @Test
    void normalizarRemoveMascara() {
        assertThat(CepUtils.normalizar("01310-100")).isEqualTo("01310100");
    }

    @Test
    void isValidoAceitaOitoDigitos() {
        assertThat(CepUtils.isValido("01310100")).isTrue();
    }

    @Test
    void isValidoRejeitaFormatoComMenosDeOitoDigitos() {
        assertThat(CepUtils.isValido("013101")).isFalse();
    }

    @Test
    void isValidoRejeitaNulo() {
        assertThat(CepUtils.isValido(null)).isFalse();
    }
}