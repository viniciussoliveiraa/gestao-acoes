package br.com.gestaoacoes.integration.instituicao;

import br.com.gestaoacoes.exception.IntegracaoExternaIndisponivelException;
import feign.FeignException;
import org.springframework.stereotype.Component;

@Component
public class BrasilApiCvmAdapter implements InstituicaoFinanceiraPort {

    private final BrasilApiCvmClient client;

    public BrasilApiCvmAdapter(BrasilApiCvmClient client) {
        this.client = client;
    }

    @Override
    public boolean validar(String cnpjNormalizado) {
        try {
            BrasilApiCvmResponse response = client.consultar(cnpjNormalizado);
            return response.ativo();
        } catch (FeignException.NotFound e) {
            return false;
        } catch (RuntimeException e) {
            throw new IntegracaoExternaIndisponivelException("Falha ao validar instituição financeira na CVM/BrasilAPI", e);
        }
    }
}