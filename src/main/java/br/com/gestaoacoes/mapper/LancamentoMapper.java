package br.com.gestaoacoes.mapper;

import br.com.gestaoacoes.dto.LancamentoResponse;
import br.com.gestaoacoes.model.Lancamento;
import org.springframework.stereotype.Component;

@Component
public class LancamentoMapper {

    public LancamentoResponse toResponse(Lancamento lancamento) {
        return new LancamentoResponse(
                lancamento.getId(),
                lancamento.getAcao().getId(),
                lancamento.getAcao().getTicker(),
                lancamento.getCorretora().getId(),
                lancamento.getCorretora().getRazaoSocial(),
                lancamento.getQuantidade(),
                lancamento.getPrecoUnitario(),
                lancamento.getDataOperacao(),
                lancamento.getCriadoEm()
        );
    }
}