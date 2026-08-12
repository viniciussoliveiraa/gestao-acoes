package br.com.gestaoacoes.repository;

import br.com.gestaoacoes.model.Acao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AcaoRepository extends JpaRepository<Acao, Long> {

    Optional<Acao> findByTicker(String ticker);
}