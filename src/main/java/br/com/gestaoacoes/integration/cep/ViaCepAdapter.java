package br.com.gestaoacoes.integration.cep;

import br.com.gestaoacoes.exception.CepNaoEncontradoException;
import br.com.gestaoacoes.exception.IntegracaoExternaIndisponivelException;
import org.springframework.stereotype.Component;

@Component
public class ViaCepAdapter implements EnderecoPort {

    private final ViaCepClient client;

    public ViaCepAdapter(ViaCepClient client) {
        this.client = client;
    }

    @Override
    public Endereco consultar(String cepNormalizado) {
        ViaCepResponse response;
        try {
            response = client.consultar(cepNormalizado);
        } catch (RuntimeException e) {
            throw new IntegracaoExternaIndisponivelException("Falha ao consultar CEP na ViaCEP", e);
        }
        if (response.naoEncontrado()) {
            throw new CepNaoEncontradoException("CEP não encontrado: " + cepNormalizado);
        }
        return new Endereco(response.logradouro(), response.bairro(), response.localidade(), response.uf());
    }
}