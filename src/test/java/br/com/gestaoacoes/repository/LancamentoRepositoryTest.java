package br.com.gestaoacoes.repository;

import br.com.gestaoacoes.model.Acao;
import br.com.gestaoacoes.model.Corretora;
import br.com.gestaoacoes.model.Lancamento;
import br.com.gestaoacoes.model.Mercado;
import br.com.gestaoacoes.model.Moeda;
import br.com.gestaoacoes.model.TipoLancamento;
import br.com.gestaoacoes.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class LancamentoRepositoryTest {

    @Autowired
    private LancamentoRepository repository;
    @Autowired
    private AcaoRepository acaoRepository;
    @Autowired
    private CorretoraRepository corretoraRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    private Acao petr4;
    private Acao vale3;
    private Corretora corretora;
    private Long usuario1Id;
    private Long usuario2Id;

    @BeforeEach
    void setUp() {
        petr4 = acaoRepository.save(novaAcao("PETR4", new BigDecimal("35.0000")));
        vale3 = acaoRepository.save(novaAcao("VALE3", new BigDecimal("60.0000")));
        corretora = corretoraRepository.save(novaCorretora("11222333000181"));
        usuario1Id = usuarioRepository.save(novoUsuario("usuario1@exemplo.com")).getId();
        usuario2Id = usuarioRepository.save(novoUsuario("usuario2@exemplo.com")).getId();
    }

    @Test
    void listaLancamentosPorUsuarioPaginado() {
        repository.save(novoLancamento(usuario1Id, petr4, TipoLancamento.COMPRA, new BigDecimal("100"), new BigDecimal("30.0000")));
        repository.save(novoLancamento(usuario2Id, petr4, TipoLancamento.COMPRA, new BigDecimal("50"), new BigDecimal("31.0000")));

        var pagina = repository.findByUsuarioId(usuario1Id, PageRequest.of(0, 10));

        assertThat(pagina.getTotalElements()).isEqualTo(1);
    }

    @Test
    void listaLancamentosOrdenadosPorAtivoEDataParaCalculoDePosicao() {
        repository.save(novoLancamento(usuario1Id, petr4, TipoLancamento.COMPRA, new BigDecimal("50"), new BigDecimal("36.0000"),
                LocalDate.of(2026, 8, 20)));
        repository.save(novoLancamento(usuario1Id, petr4, TipoLancamento.COMPRA, new BigDecimal("100"), new BigDecimal("30.0000"),
                LocalDate.of(2026, 8, 10)));
        repository.save(novoLancamento(usuario1Id, vale3, TipoLancamento.COMPRA, new BigDecimal("10"), new BigDecimal("60.0000"),
                LocalDate.of(2026, 8, 15)));

        List<Lancamento> lancamentos = repository.listarPorUsuarioOrdenadoPorAtivoEData(usuario1Id);

        assertThat(lancamentos).hasSize(3);
        assertThat(lancamentos.get(0).getAcao().getTicker()).isEqualTo("PETR4");
        assertThat(lancamentos.get(0).getDataOperacao()).isEqualTo(LocalDate.of(2026, 8, 10));
        assertThat(lancamentos.get(1).getAcao().getTicker()).isEqualTo("PETR4");
        assertThat(lancamentos.get(1).getDataOperacao()).isEqualTo(LocalDate.of(2026, 8, 20));
        assertThat(lancamentos.get(2).getAcao().getTicker()).isEqualTo("VALE3");
    }

    @Test
    void listaLancamentosPorUsuarioEAtivoIgnorandoOutrosAtivosEUsuarios() {
        repository.save(novoLancamento(usuario1Id, petr4, TipoLancamento.COMPRA, new BigDecimal("100"), new BigDecimal("30.0000")));
        repository.save(novoLancamento(usuario1Id, vale3, TipoLancamento.COMPRA, new BigDecimal("10"), new BigDecimal("60.0000")));
        repository.save(novoLancamento(usuario2Id, petr4, TipoLancamento.COMPRA, new BigDecimal("50"), new BigDecimal("31.0000")));

        List<Lancamento> lancamentos = repository.findByUsuarioIdAndAcaoId(usuario1Id, petr4.getId());

        assertThat(lancamentos).hasSize(1);
        assertThat(lancamentos.get(0).getQuantidade()).isEqualByComparingTo("100");
    }

    private Lancamento novoLancamento(Long usuarioId, Acao acao, TipoLancamento tipo, BigDecimal quantidade, BigDecimal preco) {
        return novoLancamento(usuarioId, acao, tipo, quantidade, preco, LocalDate.now());
    }

    private Lancamento novoLancamento(Long usuarioId, Acao acao, TipoLancamento tipo, BigDecimal quantidade,
                                       BigDecimal preco, LocalDate dataOperacao) {
        return new Lancamento(usuarioId, acao, corretora, tipo, quantidade, preco, dataOperacao, OffsetDateTime.now());
    }

    private Usuario novoUsuario(String email) {
        return new Usuario("Usuario Teste", email, "hash-de-senha", OffsetDateTime.now());
    }

    private Acao novaAcao(String ticker, BigDecimal cotacao) {
        return new Acao(ticker, "Empresa Teste", Mercado.BRASIL, Moeda.BRL, cotacao,
                OffsetDateTime.now(), "teste", OffsetDateTime.now());
    }

    private Corretora novaCorretora(String cnpj) {
        return new Corretora(cnpj, "Razao LTDA", "Fantasia", "contato@corretora.com", "1130000000",
                "01310100", "Av. Paulista", "1000", null, "Bela Vista", "Sao Paulo", "SP",
                "ATIVA", true, OffsetDateTime.now());
    }
}
