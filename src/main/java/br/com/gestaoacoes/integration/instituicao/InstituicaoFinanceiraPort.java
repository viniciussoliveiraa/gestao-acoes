package br.com.gestaoacoes.integration.instituicao;

public interface InstituicaoFinanceiraPort {

    /**
     * @param cnpjNormalizado CNPJ com 14 dígitos, sem máscara.
     * @return {@code true} se o CNPJ consta como corretora em funcionamento normal na CVM.
     */
    boolean validar(String cnpjNormalizado);
}