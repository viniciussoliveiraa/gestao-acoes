package br.com.gestaoacoes.mapper;

import br.com.gestaoacoes.dto.ProventoResponse;
import br.com.gestaoacoes.model.Provento;
import org.springframework.stereotype.Component;

@Component
public class ProventoMapper {

    public ProventoResponse toResponse(Provento provento) {
        return new ProventoResponse(
                provento.getId(),
                provento.getAcao().getId(),
                provento.getAcao().getTicker(),
                provento.getTipo(),
                provento.getValorTotal(),
                provento.getDataPagamento(),
                provento.getCriadoEm()
        );
    }
}