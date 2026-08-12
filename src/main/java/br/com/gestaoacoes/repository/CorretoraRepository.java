package br.com.gestaoacoes.repository;

import br.com.gestaoacoes.model.Corretora;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CorretoraRepository extends JpaRepository<Corretora, Long> {

    Optional<Corretora> findByCnpj(String cnpj);
}