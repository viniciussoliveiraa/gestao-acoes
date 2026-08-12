package br.com.gestaoacoes.integration.cotacao;

import br.com.gestaoacoes.model.Moeda;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CotacaoExterna(
        String nomeEmpresa,
        Moeda moeda,
        BigDecimal preco,
        OffsetDateTime dataHora,
        String provedor
) {
}