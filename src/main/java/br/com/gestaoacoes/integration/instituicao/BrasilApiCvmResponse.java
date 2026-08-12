package br.com.gestaoacoes.integration.instituicao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
record BrasilApiCvmResponse(String cnpj, String status) {

    private static final String STATUS_ATIVO = "EM FUNCIONAMENTO NORMAL";

    boolean ativo() {
        return STATUS_ATIVO.equalsIgnoreCase(status);
    }
}