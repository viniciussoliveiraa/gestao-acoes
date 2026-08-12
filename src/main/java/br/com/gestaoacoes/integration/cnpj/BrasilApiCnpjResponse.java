package br.com.gestaoacoes.integration.cnpj;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
record BrasilApiCnpjResponse(
        @JsonProperty("razao_social") String razaoSocial,
        @JsonProperty("nome_fantasia") String nomeFantasia,
        @JsonProperty("descricao_situacao_cadastral") String descricaoSituacaoCadastral,
        @JsonProperty("email") String email,
        @JsonProperty("ddd_telefone_1") String dddTelefone1,
        @JsonProperty("cep") String cep,
        @JsonProperty("logradouro") String logradouro,
        @JsonProperty("numero") String numero,
        @JsonProperty("complemento") String complemento,
        @JsonProperty("bairro") String bairro,
        @JsonProperty("municipio") String municipio,
        @JsonProperty("uf") String uf
) {
}