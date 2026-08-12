package br.com.gestaoacoes.integration.cotacao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record BrapiResponse(List<BrapiResult> results) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record BrapiResult(String symbol, BrapiData data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record BrapiData(String longName, String currency, BigDecimal regularMarketPrice, String regularMarketTime) {
    }
}