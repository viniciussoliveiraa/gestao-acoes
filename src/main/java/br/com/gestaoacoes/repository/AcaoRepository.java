package br.com.gestaoacoes.repository;

import br.com.gestaoacoes.model.Acao;
import br.com.gestaoacoes.model.Mercado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AcaoRepository extends JpaRepository<Acao, Long> {

    Optional<Acao> findByTickerAndMercado(String ticker, Mercado mercado);

    List<Acao> findByTicker(String ticker);
}