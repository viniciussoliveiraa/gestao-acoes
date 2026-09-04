package br.com.gestaoacoes.dto;

import java.math.BigDecimal;

public record PosicaoResponse(
        Long acaoId,
        String ticker,
        String nomeEmpresa,
        BigDecimal quantidade,
        BigDecimal precoMedio,
        BigDecimal valorInvestido,
        BigDecimal valorAtual,
        BigDecimal variacaoPercentual,
        BigDecimal resultadoRealizado
) {
}