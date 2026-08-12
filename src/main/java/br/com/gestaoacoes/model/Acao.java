package br.com.gestaoacoes.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "acao")
public class Acao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String ticker;

    @Column(name = "nome_empresa")
    private String nomeEmpresa;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Mercado mercado;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 3)
    private Moeda moeda;

    @Column(name = "cotacao_atual", nullable = false, precision = 19, scale = 4)
    private BigDecimal cotacaoAtual;

    @Column(name = "data_hora_cotacao", nullable = false)
    private OffsetDateTime dataHoraCotacao;

    @Column(name = "provedor_origem", nullable = false, length = 30)
    private String provedorOrigem;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    protected Acao() {
        // uso exclusivo do JPA
    }

    public Acao(String ticker, String nomeEmpresa, Mercado mercado, Moeda moeda, BigDecimal cotacaoAtual,
                OffsetDateTime dataHoraCotacao, String provedorOrigem, OffsetDateTime criadoEm) {
        this.ticker = ticker;
        this.nomeEmpresa = nomeEmpresa;
        this.mercado = mercado;
        this.moeda = moeda;
        this.cotacaoAtual = cotacaoAtual;
        this.dataHoraCotacao = dataHoraCotacao;
        this.provedorOrigem = provedorOrigem;
        this.criadoEm = criadoEm;
    }

    public void atualizarCotacao(BigDecimal novaCotacao, OffsetDateTime novaDataHora) {
        this.cotacaoAtual = novaCotacao;
        this.dataHoraCotacao = novaDataHora;
    }

    public Long getId() {
        return id;
    }

    public String getTicker() {
        return ticker;
    }

    public String getNomeEmpresa() {
        return nomeEmpresa;
    }

    public Mercado getMercado() {
        return mercado;
    }

    public Moeda getMoeda() {
        return moeda;
    }

    public BigDecimal getCotacaoAtual() {
        return cotacaoAtual;
    }

    public OffsetDateTime getDataHoraCotacao() {
        return dataHoraCotacao;
    }

    public String getProvedorOrigem() {
        return provedorOrigem;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }
}