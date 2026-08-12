package br.com.gestaoacoes.integration.cep;

public record Endereco(
        String logradouro,
        String bairro,
        String cidade,
        String uf
) {
}