package br.com.gestaoacoes.util;

public final class CepUtils {

    private CepUtils() {
    }

    public static String normalizar(String cep) {
        if (cep == null) {
            return "";
        }
        return cep.replaceAll("\\D", "");
    }

    public static boolean isValido(String cepNormalizado) {
        return cepNormalizado != null && cepNormalizado.matches("\\d{8}");
    }
}