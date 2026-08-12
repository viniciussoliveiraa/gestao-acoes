package br.com.gestaoacoes.integration.cnpj;

public record DadosCnpj(
        String razaoSocial,
        String nomeFantasia,
        String situacaoCadastral,
        String email,
        String telefone,
        String cep,
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String cidade,
        String uf
) {
}