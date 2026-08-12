package br.com.gestaoacoes.integration.cnpj;

public interface CnpjDataPort {

    /**
     * @param cnpjNormalizado CNPJ com 14 dígitos, sem máscara.
     */
    DadosCnpj consultar(String cnpjNormalizado);
}