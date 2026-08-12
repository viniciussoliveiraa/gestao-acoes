package br.com.gestaoacoes.mapper;

import br.com.gestaoacoes.dto.CorretoraResponse;
import br.com.gestaoacoes.model.Corretora;
import org.springframework.stereotype.Component;

@Component
public class CorretoraMapper {

    public CorretoraResponse toResponse(Corretora corretora) {
        return new CorretoraResponse(
                corretora.getId(),
                corretora.getCnpj(),
                corretora.getRazaoSocial(),
                corretora.getNomeFantasia(),
                corretora.getEmail(),
                corretora.getTelefone(),
                corretora.getCep(),
                corretora.getLogradouro(),
                corretora.getNumero(),
                corretora.getComplemento(),
                corretora.getBairro(),
                corretora.getCidade(),
                corretora.getUf(),
                corretora.getSituacaoCadastral(),
                corretora.isValidadaCvm(),
                corretora.getCriadoEm()
        );
    }
}