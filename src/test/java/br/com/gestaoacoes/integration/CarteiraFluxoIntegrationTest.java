package br.com.gestaoacoes.integration;

import br.com.gestaoacoes.model.Acao;
import br.com.gestaoacoes.model.Corretora;
import br.com.gestaoacoes.model.Mercado;
import br.com.gestaoacoes.model.Moeda;
import br.com.gestaoacoes.repository.AcaoRepository;
import br.com.gestaoacoes.repository.CorretoraRepository;
import br.com.gestaoacoes.repository.LancamentoRepository;
import br.com.gestaoacoes.repository.ProventoRepository;
import br.com.gestaoacoes.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Deliberadamente SEM {@code @Transactional} na classe: os outros testes de fluxo usam
 * {@code @Transactional} para rollback automático, mas isso mantém uma única sessão Hibernate
 * aberta durante toda a requisição MockMvc e mascara problemas de {@code open-in-view=false}
 * (como {@link org.hibernate.LazyInitializationException} ao acessar uma associação
 * {@code @ManyToOne(LAZY)} fora da transação). Este teste reproduz os limites reais de
 * transação por requisição — por isso a limpeza dos dados criados é manual no {@link #limpar()},
 * em vez de depender de rollback automático (o H2 em memória é compartilhado entre as classes de
 * teste dentro do mesmo fork do Surefire).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class CarteiraFluxoIntegrationTest {

    private static final String TICKER = "ZTST1";
    private static final String CNPJ = "99999999000191";
    private static final String EMAIL = "ana.fluxo.carteira@exemplo.com";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AcaoRepository acaoRepository;
    @Autowired
    private CorretoraRepository corretoraRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private LancamentoRepository lancamentoRepository;
    @Autowired
    private ProventoRepository proventoRepository;

    // Instanciado diretamente, não injetado: o bean gerenciado pelo Spring Boot é
    // tools.jackson.databind.json.JsonMapper (Jackson 3), não com.fasterxml...ObjectMapper —
    // ver nota equivalente em ProblemDetailJson.
    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void limpar() {
        // Ordem respeita as FKs (lancamento/provento referenciam acao/corretora/usuario).
        lancamentoRepository.deleteAll();
        proventoRepository.deleteAll();
        acaoRepository.findByTicker(TICKER).ifPresent(acaoRepository::delete);
        corretoraRepository.findByCnpj(CNPJ).ifPresent(corretoraRepository::delete);
        usuarioRepository.findByEmail(EMAIL).ifPresent(usuarioRepository::delete);
    }

    @Test
    void fluxoCompletoDeLancamentoEProventoNaoLancaLazyInitializationException() throws Exception {
        Acao acao = acaoRepository.save(new Acao(TICKER, "Empresa Teste", Mercado.BRASIL, Moeda.BRL,
                new BigDecimal("35.0000"), OffsetDateTime.now(), "teste", OffsetDateTime.now()));
        Corretora corretora = corretoraRepository.save(new Corretora(CNPJ, "Razao LTDA", "Fantasia",
                null, null, "01310100", "Av. Paulista", "1000", null, "Bela Vista", "Sao Paulo", "SP",
                "ATIVA", true, OffsetDateTime.now()));

        mockMvc.perform(post("/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Ana\",\"email\":\"" + EMAIL + "\",\"senha\":\"senhaSegura123\"}"))
                .andExpect(status().isCreated());

        String respostaLogin = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + EMAIL + "\",\"senha\":\"senhaSegura123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(respostaLogin).get("token").asText();

        String corpoLancamento = """
                {"acaoId": %d, "corretoraId": %d, "quantidade": 100, "precoUnitario": 32.50, "dataOperacao": "2026-08-10"}
                """.formatted(acao.getId(), corretora.getId());
        mockMvc.perform(post("/carteira/lancamentos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoLancamento))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tickerAcao").value(TICKER))
                .andExpect(jsonPath("$.tipo").value("COMPRA"));

        // Venda acima do saldo disponível (100) deve ser rejeitada e não persistida.
        String corpoVendaAcimaDoSaldo = """
                {"acaoId": %d, "corretoraId": %d, "tipo": "VENDA", "quantidade": 150, "precoUnitario": 35.00, "dataOperacao": "2026-08-20"}
                """.formatted(acao.getId(), corretora.getId());
        mockMvc.perform(post("/carteira/lancamentos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoVendaAcimaDoSaldo))
                .andExpect(status().isUnprocessableEntity());

        // Venda parcial dentro do saldo.
        String corpoVenda = """
                {"acaoId": %d, "corretoraId": %d, "tipo": "VENDA", "quantidade": 40, "precoUnitario": 35.00, "dataOperacao": "2026-08-20"}
                """.formatted(acao.getId(), corretora.getId());
        mockMvc.perform(post("/carteira/lancamentos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoVenda))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("VENDA"));

        // Reproduz o bug: numa requisição HTTP real (sem @Transactional no teste), o
        // LancamentoMapper roda fora da transação do repositório e precisa encontrar acao/
        // corretora já carregados via JOIN FETCH.
        mockMvc.perform(get("/carteira/lancamentos").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].tickerAcao").value(everyItem(is(TICKER))))
                .andExpect(jsonPath("$.content[*].razaoSocialCorretora").value(everyItem(is("Razao LTDA"))));

        // Preço médio (compra) mantido em 32,50 mesmo após a venda; quantidade líquida 60.
        String respostaPosicoes = mockMvc.perform(get("/carteira/posicoes").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ticker").value(TICKER))
                .andReturn().getResponse().getContentAsString();
        var posicao = objectMapper.readTree(respostaPosicoes).get(0);
        assertThat(new BigDecimal(posicao.get("quantidade").asText())).isEqualByComparingTo("60");
        assertThat(new BigDecimal(posicao.get("precoMedio").asText())).isEqualByComparingTo("32.5000");
        assertThat(new BigDecimal(posicao.get("resultadoRealizado").asText())).isEqualByComparingTo("100.0000");

        String corpoProvento = """
                {"acaoId": %d, "tipo": "DIVIDENDO", "valorTotal": 45.90, "dataPagamento": "2026-07-15"}
                """.formatted(acao.getId());
        mockMvc.perform(post("/proventos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoProvento))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/proventos").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].tickerAcao").value(TICKER));
    }
}