package br.com.gestaoacoes.integration.cotacao;

public interface CotacaoPort {

    /**
     * @param tickerNormalizado ticker já normalizado (maiúsculas, sem espaços).
     */
    CotacaoExterna obterCotacao(String tickerNormalizado);
}