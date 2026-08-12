package br.com.gestaoacoes.util;

import java.util.Locale;

public final class TickerUtils {

    private TickerUtils() {
    }

    public static String normalizar(String ticker) {
        if (ticker == null) {
            return "";
        }
        return ticker.trim().toUpperCase(Locale.ROOT);
    }
}