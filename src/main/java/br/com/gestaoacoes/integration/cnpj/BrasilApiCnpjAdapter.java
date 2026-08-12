package br.com.gestaoacoes.integration.cnpj;

import br.com.gestaoacoes.exception.CnpjNaoEncontradoException;
import br.com.gestaoacoes.exception.IntegracaoExternaIndisponivelException;
import feign.FeignException;
import org.springframework.stereotype.Component;

@Component
public class BrasilApiCnpjAdapter implements CnpjDataPort {

    private final BrasilApiCnpjClient client;

    public BrasilApiCnpjAdapter(BrasilApiCnpjClient client) {
        this.client = client;
    }

    @Override
    public DadosCnpj consultar(String cnpjNormalizado) {
        BrasilApiCnpjResponse response;
        try {
            response = client.consultar(cnpjNormalizado);
        } catch (FeignException.NotFound e) {
            throw new CnpjNaoEncontradoException("CNPJ não encontrado na base da BrasilAPI: " + mascarar(cnpjNormalizado));
        } catch (RuntimeException e) {
            throw new IntegracaoExternaIndisponivelException("Falha ao consultar dados cadastrais do CNPJ na BrasilAPI", e);
        }
        return new DadosCnpj(
                response.razaoSocial(),
                response.nomeFantasia(),
                response.descricaoSituacaoCadastral(),
                response.email(),
                response.dddTelefone1(),
                response.cep(),
                response.logradouro(),
                response.numero(),
                response.complemento(),
                response.bairro(),
                response.municipio(),
                response.uf()
        );
    }

    private String mascarar(String cnpj) {
        if (cnpj == null || cnpj.length() != 14) {
            return "***";
        }
        return cnpj.substring(0, 2) + "***" + cnpj.substring(11);
    }
}