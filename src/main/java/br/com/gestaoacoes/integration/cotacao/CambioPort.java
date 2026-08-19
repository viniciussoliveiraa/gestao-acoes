package br.com.gestaoacoes.integration.cotacao;

import java.math.BigDecimal;

public interface CambioPort {

    /**
     * @return quantos reais equivalem a 1 dólar, na cotação mais recente disponível.
     */
    BigDecimal obterCotacaoUsdParaBrl();
}
