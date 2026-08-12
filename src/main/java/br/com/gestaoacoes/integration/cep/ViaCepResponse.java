package br.com.gestaoacoes.integration.cep;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
record ViaCepResponse(
        String logradouro,
        String bairro,
        String localidade,
        String uf,
        String erro
) {
    boolean naoEncontrado() {
        return "true".equalsIgnoreCase(erro);
    }
}