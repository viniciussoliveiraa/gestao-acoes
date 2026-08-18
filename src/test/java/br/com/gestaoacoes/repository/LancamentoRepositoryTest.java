package br.com.gestaoacoes.repository;

import br.com.gestaoacoes.model.Acao;
import br.com.gestaoacoes.model.Corretora;
import br.com.gestaoacoes.model.Lancamento;
import br.com.gestaoacoes.model.Mercado;
import br.com.gestaoacoes.model.Moeda;
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
        repository.save(novoLancamento(usuario1Id, petr4, new BigDecimal("100"), new BigDecimal("30.0000")));
        repository.save(novoLancamento(usuario2Id, petr4, new BigDecimal("50"), new BigDecimal("31.0000")));

        var pagina = repository.findByUsuarioId(usuario1Id, PageRequest.of(0, 10));

        assertThat(pagina.getTotalElements()).isEqualTo(1);
    }

    @Test
    void agregaPosicaoDeUmUnicoLancamento() {
        repository.save(novoLancamento(usuario1Id, petr4, new BigDecimal("100"), new BigDecimal("32.5000")));

        List<PosicaoAgregada> posicoes = repository.agregarPosicoesPorUsuario(usuario1Id);

        assertThat(posicoes).hasSize(1);
        PosicaoAgregada posicao = posicoes.get(0);
        assertThat(posicao.acao().getTicker()).isEqualTo("PETR4");
        assertThat(posicao.quantidadeTotal()).isEqualByComparingTo("100");
        assertThat(posicao.valorInvestidoTotal()).isEqualByComparingTo("3250.0000");
    }

    @Test
    void agregaPosicaoConsolidandoMultiplosLancamentosDoMesmoAtivo() {
        repository.save(novoLancamento(usuario1Id, petr4, new BigDecimal("100"), new BigDecimal("30.0000")));
        repository.save(novoLancamento(usuario1Id, petr4, new BigDecimal("50"), new BigDecimal("36.0000")));

        List<PosicaoAgregada> posicoes = repository.agregarPosicoesPorUsuario(usuario1Id);

        assertThat(posicoes).hasSize(1);
        PosicaoAgregada posicao = posicoes.get(0);
        assertThat(posicao.quantidadeTotal()).isEqualByComparingTo("150");
        assertThat(posicao.valorInvestidoTotal()).isEqualByComparingTo("4800.0000");
    }

    @Test
    void agregaPosicoesSeparadasPorAtivoDiferente() {
        repository.save(novoLancamento(usuario1Id, petr4, new BigDecimal("100"), new BigDecimal("30.0000")));
        repository.save(novoLancamento(usuario1Id, vale3, new BigDecimal("10"), new BigDecimal("60.0000")));

        List<PosicaoAgregada> posicoes = repository.agregarPosicoesPorUsuario(usuario1Id);

        assertThat(posicoes).hasSize(2);
    }

    @Test
    void naoRetornaPosicoesDeOutroUsuario() {
        repository.save(novoLancamento(usuario1Id, petr4, new BigDecimal("100"), new BigDecimal("30.0000")));

        List<PosicaoAgregada> posicoesUsuario2 = repository.agregarPosicoesPorUsuario(usuario2Id);

        assertThat(posicoesUsuario2).isEmpty();
    }

    private Lancamento novoLancamento(Long usuarioId, Acao acao, BigDecimal quantidade, BigDecimal preco) {
        return new Lancamento(usuarioId, acao, corretora, quantidade, preco, LocalDate.now(), OffsetDateTime.now());
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