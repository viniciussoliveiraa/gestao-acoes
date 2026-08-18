package br.com.gestaoacoes.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "lancamento")
public class Lancamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "acao_id", nullable = false)
    private Acao acao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "corretora_id", nullable = false)
    private Corretora corretora;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private TipoLancamento tipo;

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal quantidade;

    @Column(name = "preco_unitario", nullable = false, precision = 19, scale = 4)
    private BigDecimal precoUnitario;

    @Column(name = "data_operacao", nullable = false)
    private LocalDate dataOperacao;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    protected Lancamento() {
        // uso exclusivo do JPA
    }

    public Lancamento(Long usuarioId, Acao acao, Corretora corretora, BigDecimal quantidade,
                       BigDecimal precoUnitario, LocalDate dataOperacao, OffsetDateTime criadoEm) {
        this.usuarioId = usuarioId;
        this.acao = acao;
        this.corretora = corretora;
        this.tipo = TipoLancamento.COMPRA;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
        this.dataOperacao = dataOperacao;
        this.criadoEm = criadoEm;
    }

    public Long getId() {
        return id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public Acao getAcao() {
        return acao;
    }

    public Corretora getCorretora() {
        return corretora;
    }

    public TipoLancamento getTipo() {
        return tipo;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public LocalDate getDataOperacao() {
        return dataOperacao;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }
}