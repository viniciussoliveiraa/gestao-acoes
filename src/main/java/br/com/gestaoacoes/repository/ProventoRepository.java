package br.com.gestaoacoes.repository;

import br.com.gestaoacoes.model.Provento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProventoRepository extends JpaRepository<Provento, Long> {

    // JOIN FETCH explícito pelo mesmo motivo documentado em LancamentoRepository: acao é
    // @ManyToOne(LAZY) e o projeto roda com spring.jpa.open-in-view=false.
    @Query(value = "SELECT p FROM Provento p JOIN FETCH p.acao WHERE p.usuarioId = :usuarioId ORDER BY p.dataPagamento DESC",
            countQuery = "SELECT COUNT(p) FROM Provento p WHERE p.usuarioId = :usuarioId")
    Page<Provento> findByUsuarioIdOrderByDataPagamentoDesc(@Param("usuarioId") Long usuarioId, Pageable pageable);
}