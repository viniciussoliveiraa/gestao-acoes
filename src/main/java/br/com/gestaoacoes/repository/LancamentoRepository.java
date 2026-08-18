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

    @Query("""
            SELECT new br.com.gestaoacoes.repository.PosicaoAgregada(l.acao, SUM(l.quantidade), SUM(l.quantidade * l.precoUnitario))
            FROM Lancamento l
            WHERE l.usuarioId = :usuarioId
            GROUP BY l.acao
            """)
    List<PosicaoAgregada> agregarPosicoesPorUsuario(@Param("usuarioId") Long usuarioId);
}