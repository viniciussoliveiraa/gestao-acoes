package br.com.gestaoacoes.dto;

/**
 * Prévia dos dados de uma corretora a partir do CNPJ ({@code GET /corretoras/consulta-cnpj/{cnpj}}),
 * usada pelo formulário para se preencher sozinho antes do cadastro. Nada é gravado: o cadastro
 * continua sendo o {@code POST /corretoras}, com as mesmas validações de sempre.
 *
 * @param validadaCvm se o CNPJ consta como instituição em funcionamento normal na CVM — quando
 *                    {@code false}, o cadastro seria recusado ({@code 422}).
 */
public record ConsultaCnpjResponse(
        String cnpj,
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
        String uf,
        boolean validadaCvm
) {
}
