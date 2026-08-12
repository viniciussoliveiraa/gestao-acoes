package br.com.gestaoacoes.mapper;

import br.com.gestaoacoes.dto.AcaoResponse;
import br.com.gestaoacoes.model.Acao;
import org.springframework.stereotype.Component;

@Component
public class AcaoMapper {

    public AcaoResponse toResponse(Acao acao) {
        return new AcaoResponse(
                acao.getId(),
                acao.getTicker(),
                acao.getNomeEmpresa(),
                acao.getMercado(),
                acao.getMoeda(),
                acao.getCotacaoAtual(),
                acao.getDataHoraCotacao(),
                acao.getProvedorOrigem(),
                acao.getCriadoEm()
        );
    }
}