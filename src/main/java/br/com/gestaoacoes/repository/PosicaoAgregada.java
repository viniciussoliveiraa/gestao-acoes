package br.com.gestaoacoes.repository;

import br.com.gestaoacoes.model.Acao;

import java.math.BigDecimal;

/**
 * Projeção usada por {@link LancamentoRepository#agregarPosicoesPorUsuario} — quantidade total e
 * valor investido total (soma de quantidade × preço) de um ativo, agregados a partir dos
 * lançamentos de um usuário. O cálculo de preço médio/valor atual/variação fica no service, que
 * também tem acesso à cotação atual da ação.
 */
public record PosicaoAgregada(Acao acao, BigDecimal quantidadeTotal, BigDecimal valorInvestidoTotal) {
}