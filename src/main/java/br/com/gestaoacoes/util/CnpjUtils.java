package br.com.gestaoacoes.util;

public final class CnpjUtils {

    private static final int[] PESO_DV1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] PESO_DV2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private CnpjUtils() {
    }

    public static String normalizar(String cnpj) {
        if (cnpj == null) {
            return "";
        }
        return cnpj.replaceAll("\\D", "");
    }

    public static boolean isValido(String cnpjNormalizado) {
        if (cnpjNormalizado == null || cnpjNormalizado.length() != 14 || todosDigitosIguais(cnpjNormalizado)) {
            return false;
        }
        int[] digitos = cnpjNormalizado.chars().map(c -> c - '0').toArray();
        int dv1 = calcularDigitoVerificador(digitos, PESO_DV1);
        if (dv1 != digitos[12]) {
            return false;
        }
        int dv2 = calcularDigitoVerificador(digitos, PESO_DV2);
        return dv2 == digitos[13];
    }

    private static boolean todosDigitosIguais(String cnpj) {
        return cnpj.chars().distinct().count() == 1;
    }

    private static int calcularDigitoVerificador(int[] digitos, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += digitos[i] * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}