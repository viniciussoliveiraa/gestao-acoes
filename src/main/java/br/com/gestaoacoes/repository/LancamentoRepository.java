package br.com.gestaoacoes.repository;

import br.com.gestaoacoes.model.Lancamento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LancamentoRepository extends JpaRepository<Lancamento, Long> {

    // JOIN FETCH explícito: acao/corretora são @ManyToOne(LAZY) e o projeto roda com
    // spring.jpa.open-in-view=false, então o mapper (fora da transação) precisa encontrar essas
    // associações já carregadas — sem isso, LancamentoMapper.toResponse lança
    // LazyInitializationException ao chamar getAcao()/getCorretora().
    @Query(value = "SELECT l FROM Lancamento l JOIN FETCH l.acao JOIN FETCH l.corretora WHERE l.usuarioId = :usuarioId",
            countQuery = "SELECT COUNT(l) FROM Lancamento l WHERE l.usuarioId = :usuarioId")
    Page<Lancamento> findByUsuarioId(@Param("usuarioId") Long usuarioId, Pageable pageable);

    // Usada por CarteiraService para calcular a posição de cada ativo por custo médio ponderado:
    // o cálculo é order-dependent (venda não altera o preço médio, só compra), então os
    // lançamentos precisam chegar já ordenados cronologicamente por ativo — ver design.md da
    // mudança "adicionar-venda-carteira".
    @Query("SELECT l FROM Lancamento l JOIN FETCH l.acao JOIN FETCH l.corretora " +
            "WHERE l.usuarioId = :usuarioId " +
            "ORDER BY l.acao.id ASC, l.dataOperacao ASC, l.criadoEm ASC")
    List<Lancamento> listarPorUsuarioOrdenadoPorAtivoEData(@Param("usuarioId") Long usuarioId);

    // Usada para validar saldo disponível antes de registrar uma venda — não precisa de ordem,
    // a soma líquida (compra - venda) independe da ordem de processamento.
    List<Lancamento> findByUsuarioIdAndAcaoId(Long usuarioId, Long acaoId);
}
