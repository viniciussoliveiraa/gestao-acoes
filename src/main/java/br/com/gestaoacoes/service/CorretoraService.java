package br.com.gestaoacoes.service;

import br.com.gestaoacoes.dto.CorretoraRequest;
import br.com.gestaoacoes.exception.CepInvalidoException;
import br.com.gestaoacoes.exception.CnpjInvalidoException;
import br.com.gestaoacoes.exception.CorretoraDuplicadaException;
import br.com.gestaoacoes.exception.InstituicaoNaoValidadaException;
import br.com.gestaoacoes.exception.RecursoNaoEncontradoException;
import br.com.gestaoacoes.integration.cep.Endereco;
import br.com.gestaoacoes.integration.cep.EnderecoPort;
import br.com.gestaoacoes.integration.cnpj.CnpjDataPort;
import br.com.gestaoacoes.integration.cnpj.DadosCnpj;
import br.com.gestaoacoes.integration.instituicao.InstituicaoFinanceiraPort;
import br.com.gestaoacoes.model.Corretora;
import br.com.gestaoacoes.repository.CorretoraRepository;
import br.com.gestaoacoes.util.CepUtils;
import br.com.gestaoacoes.util.CnpjUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class CorretoraService {

    private final CorretoraRepository repository;
    private final CnpjDataPort cnpjDataPort;
    private final InstituicaoFinanceiraPort instituicaoFinanceiraPort;
    private final EnderecoPort enderecoPort;

    public CorretoraService(CorretoraRepository repository, CnpjDataPort cnpjDataPort,
                             InstituicaoFinanceiraPort instituicaoFinanceiraPort, EnderecoPort enderecoPort) {
        this.repository = repository;
        this.cnpjDataPort = cnpjDataPort;
        this.instituicaoFinanceiraPort = instituicaoFinanceiraPort;
        this.enderecoPort = enderecoPort;
    }

    public Corretora registrar(CorretoraRequest request) {
        String cnpjNormalizado = CnpjUtils.normalizar(request.cnpj());
        if (!CnpjUtils.isValido(cnpjNormalizado)) {
            throw new CnpjInvalidoException("CNPJ com formato ou dígitos verificadores inválidos");
        }
        if (repository.findByCnpj(cnpjNormalizado).isPresent()) {
            throw new CorretoraDuplicadaException("Já existe uma corretora cadastrada com este CNPJ");
        }

        String cepNormalizado = CepUtils.normalizar(request.cep());
        if (!CepUtils.isValido(cepNormalizado)) {
            throw new CepInvalidoException("CEP deve conter 8 dígitos");
        }

        DadosCnpj dadosCnpj = cnpjDataPort.consultar(cnpjNormalizado);

        boolean validada = instituicaoFinanceiraPort.validar(cnpjNormalizado);
        if (!validada) {
            throw new InstituicaoNaoValidadaException(
                    "CNPJ não consta como instituição em funcionamento normal na CVM");
        }

        Endereco endereco = enderecoPort.consultar(cepNormalizado);

        String email = request.email() != null ? request.email() : dadosCnpj.email();
        String telefone = request.telefone() != null ? request.telefone() : dadosCnpj.telefone();

        Corretora corretora = new Corretora(
                cnpjNormalizado,
                dadosCnpj.razaoSocial(),
                dadosCnpj.nomeFantasia(),
                email,
                telefone,
                cepNormalizado,
                endereco.logradouro(),
                request.numero(),
                request.complemento(),
                endereco.bairro(),
                endereco.cidade(),
                endereco.uf(),
                dadosCnpj.situacaoCadastral(),
                true,
                OffsetDateTime.now()
        );

        return repository.save(corretora);
    }

    public Page<Corretora> listar(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Corretora buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Corretora não encontrada: id " + id));
    }

    public Corretora buscarPorCnpj(String cnpj) {
        String cnpjNormalizado = CnpjUtils.normalizar(cnpj);
        return repository.findByCnpj(cnpjNormalizado)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Corretora não encontrada para o CNPJ informado"));
    }
}