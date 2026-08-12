package br.com.gestaoacoes.integration.cotacao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
record AlphaVantageResponse(
        @JsonProperty("Global Quote") GlobalQuote globalQuote,
        @JsonProperty("Information") String information,
        @JsonProperty("Note") String note,
        @JsonProperty("Error Message") String errorMessage
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GlobalQuote(
            @JsonProperty("01. symbol") String symbol,
            @JsonProperty("05. price") String price,
            @JsonProperty("07. latest trading day") String latestTradingDay
    ) {
    }

    boolean limiteOuIndisponivel() {
        return information != null || note != null;
    }

    boolean semCotacao() {
        return globalQuote == null || globalQuote.price() == null || globalQuote.price().isBlank();
    }
}