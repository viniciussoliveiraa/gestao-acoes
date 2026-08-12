package br.com.gestaoacoes.dto;

import br.com.gestaoacoes.model.Mercado;
import br.com.gestaoacoes.model.Moeda;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AcaoResponse(
        Long id,
        String ticker,
        String nomeEmpresa,
        Mercado mercado,
        Moeda moeda,
        BigDecimal cotacaoAtual,
        OffsetDateTime dataHoraCotacao,
        String provedorOrigem,
        OffsetDateTime criadoEm
) {
}