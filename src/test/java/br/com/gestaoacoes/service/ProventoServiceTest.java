package br.com.gestaoacoes.service;

import br.com.gestaoacoes.dto.ProventoRequest;
import br.com.gestaoacoes.exception.RecursoNaoEncontradoException;
import br.com.gestaoacoes.model.Acao;
import br.com.gestaoacoes.model.Mercado;
import br.com.gestaoacoes.model.Moeda;
import br.com.gestaoacoes.model.Provento;
import br.com.gestaoacoes.model.TipoProvento;
import br.com.gestaoacoes.repository.AcaoRepository;
import br.com.gestaoacoes.repository.ProventoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProventoServiceTest {

    @Mock
    private ProventoRepository repository;
    @Mock
    private AcaoRepository acaoRepository;

    private ProventoService service() {
        return new ProventoService(repository, acaoRepository);
    }

    @Test
    void registrarComSucessoPersisteAssociadoAoUsuario() {
        ProventoService service = service();
        Acao acao = acao();
        ProventoRequest request = new ProventoRequest(1L, TipoProvento.DIVIDENDO, new BigDecimal("45.90"), LocalDate.now());
        when(acaoRepository.findById(1L)).thenReturn(Optional.of(acao));
        when(repository.save(any(Provento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Provento provento = service.registrar(42L, request);

        assertThat(provento.getUsuarioId()).isEqualTo(42L);
        assertThat(provento.getAcao()).isEqualTo(acao);
    }

    @Test
    void registrarComAcaoInexistenteLancaExcecao() {
        ProventoService service = service();
        ProventoRequest request = new ProventoRequest(99L, TipoProvento.DIVIDENDO, new BigDecimal("45.90"), LocalDate.now());
        when(acaoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrar(42L, request))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(repository, never()).save(any());
    }

    private Acao acao() {
        return new Acao("PETR4", "Petrobras", Mercado.BRASIL, Moeda.BRL, new BigDecimal("35.0000"),
                OffsetDateTime.now(), "teste", OffsetDateTime.now());
    }
}