package br.com.gestaoacoes.service;

import br.com.gestaoacoes.dto.CorretoraRequest;
import br.com.gestaoacoes.exception.CepInvalidoException;
import br.com.gestaoacoes.exception.CnpjInvalidoException;
import br.com.gestaoacoes.exception.CorretoraDuplicadaException;
import br.com.gestaoacoes.exception.InstituicaoNaoValidadaException;
import br.com.gestaoacoes.exception.IntegracaoExternaIndisponivelException;
import br.com.gestaoacoes.integration.cep.Endereco;
import br.com.gestaoacoes.integration.cep.EnderecoPort;
import br.com.gestaoacoes.integration.cnpj.CnpjDataPort;
import br.com.gestaoacoes.integration.cnpj.DadosCnpj;
import br.com.gestaoacoes.integration.instituicao.InstituicaoFinanceiraPort;
import br.com.gestaoacoes.model.Corretora;
import br.com.gestaoacoes.repository.CorretoraRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CorretoraServiceTest {

    @Mock
    private CorretoraRepository repository;
    @Mock
    private CnpjDataPort cnpjDataPort;
    @Mock
    private InstituicaoFinanceiraPort instituicaoFinanceiraPort;
    @Mock
    private EnderecoPort enderecoPort;

    private CorretoraService service;

    private CorretoraService service() {
        return new CorretoraService(repository, cnpjDataPort, instituicaoFinanceiraPort, enderecoPort);
    }

    @Test
    void registrarComSucessoPersisteCorretora() {
        service = service();
        CorretoraRequest request = new CorretoraRequest("11.222.333/0001-81", "01310-100", "1000", null, null, null);
        when(repository.findByCnpj("11222333000181")).thenReturn(Optional.empty());
        when(cnpjDataPort.consultar("11222333000181")).thenReturn(new DadosCnpj(
                "Razao LTDA", "Fantasia", "ATIVA", "contato@empresa.com", "1130000000",
                "01310100", "Av. Paulista", "1000", "", "Bela Vista", "Sao Paulo", "SP"));
        when(instituicaoFinanceiraPort.validar("11222333000181")).thenReturn(true);
        when(enderecoPort.consultar("01310100")).thenReturn(new Endereco("Av. Paulista", "Bela Vista", "Sao Paulo", "SP"));
        when(repository.save(any(Corretora.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Corretora corretora = service.registrar(request);

        assertThat(corretora.getCnpj()).isEqualTo("11222333000181");
        assertThat(corretora.getRazaoSocial()).isEqualTo("Razao LTDA");
        assertThat(corretora.isValidadaCvm()).isTrue();
    }

    @Test
    void cnpjInvalidoNaoConsultaProvedores() {
        service = service();
        CorretoraRequest request = new CorretoraRequest("11222333000180", "01310100", null, null, null, null);

        assertThatThrownBy(() -> service.registrar(request)).isInstanceOf(CnpjInvalidoException.class);

        verifyNoInteractions(cnpjDataPort, instituicaoFinanceiraPort, enderecoPort);
    }

    @Test
    void cnpjDuplicadoNaoConsultaProvedores() {
        service = service();
        CorretoraRequest request = new CorretoraRequest("11222333000181", "01310100", null, null, null, null);
        when(repository.findByCnpj("11222333000181")).thenReturn(Optional.of(mockCorretora()));

        assertThatThrownBy(() -> service.registrar(request)).isInstanceOf(CorretoraDuplicadaException.class);

        verifyNoInteractions(cnpjDataPort, instituicaoFinanceiraPort, enderecoPort);
    }

    @Test
    void cepInvalidoNaoConsultaEndereco() {
        service = service();
        CorretoraRequest request = new CorretoraRequest("11222333000181", "123", null, null, null, null);
        when(repository.findByCnpj("11222333000181")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrar(request)).isInstanceOf(CepInvalidoException.class);

        verifyNoInteractions(cnpjDataPort, instituicaoFinanceiraPort, enderecoPort);
    }

    @Test
    void instituicaoNaoValidadaRejeitaCadastroSemConsultarEndereco() {
        service = service();
        CorretoraRequest request = new CorretoraRequest("11222333000181", "01310100", null, null, null, null);
        when(repository.findByCnpj("11222333000181")).thenReturn(Optional.empty());
        when(cnpjDataPort.consultar("11222333000181")).thenReturn(new DadosCnpj(
                "Razao LTDA", "Fantasia", "ATIVA", null, null, null, null, null, null, null, null, null));
        when(instituicaoFinanceiraPort.validar("11222333000181")).thenReturn(false);

        assertThatThrownBy(() -> service.registrar(request)).isInstanceOf(InstituicaoNaoValidadaException.class);

        verify(enderecoPort, never()).consultar(anyString());
        verify(repository, never()).save(any());
    }

    @Test
    void falhaExternaAposEtapasAnterioresNaoPersisteNada() {
        service = service();
        CorretoraRequest request = new CorretoraRequest("11222333000181", "01310100", null, null, null, null);
        when(repository.findByCnpj("11222333000181")).thenReturn(Optional.empty());
        when(cnpjDataPort.consultar("11222333000181")).thenReturn(new DadosCnpj(
                "Razao LTDA", "Fantasia", "ATIVA", null, null, null, null, null, null, null, null, null));
        when(instituicaoFinanceiraPort.validar("11222333000181")).thenReturn(true);
        when(enderecoPort.consultar("01310100")).thenThrow(new IntegracaoExternaIndisponivelException("indisponível"));

        assertThatThrownBy(() -> service.registrar(request)).isInstanceOf(IntegracaoExternaIndisponivelException.class);

        verify(repository, never()).save(any());
    }

    private Corretora mockCorretora() {
        return new Corretora("11222333000181", "Razao", "Fantasia", null, null, "01310100",
                "Av. Paulista", "1000", null, "Bela Vista", "Sao Paulo", "SP", "ATIVA", true,
                java.time.OffsetDateTime.now());
    }
}