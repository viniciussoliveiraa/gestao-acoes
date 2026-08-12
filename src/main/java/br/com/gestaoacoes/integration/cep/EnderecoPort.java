package br.com.gestaoacoes.integration.cep;

public interface EnderecoPort {

    /**
     * @param cepNormalizado CEP com 8 dígitos, sem máscara. O chamador já deve
     *                       ter validado o formato antes de invocar este método.
     */
    Endereco consultar(String cepNormalizado);
}