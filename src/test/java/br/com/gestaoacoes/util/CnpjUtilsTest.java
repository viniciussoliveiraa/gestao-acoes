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

    @Test
    void normalizarNuloRetornaTextoVazio() {
        assertThat(CnpjUtils.normalizar(null)).isEmpty();
    }

    @Test
    void isValidoRejeitaPrimeiroDigitoVerificadorIncorreto() {
        // correto e 11222333000181: aqui o 1o DV (8) foi trocado por 9
        assertThat(CnpjUtils.isValido("11222333000191")).isFalse();
    }

    @Test
    void isValidoAceitaCnpjCujoDigitoVerificadorEZero() {
        // CNPJ real da XP Investimentos: o 1o DV e 0 (resto da divisao por 11 menor que 2)
        assertThat(CnpjUtils.isValido("02332886000104")).isTrue();
    }
}