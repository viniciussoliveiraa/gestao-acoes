package br.com.gestaoacoes.integration.cotacao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
record TwelveDataResponse(String symbol, String name, String currency, String close, Long timestamp, String status) {

    boolean erro() {
        return "error".equalsIgnoreCase(status) || close == null;
    }
}