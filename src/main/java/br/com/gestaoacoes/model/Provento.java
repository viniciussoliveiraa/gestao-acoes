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
@Table(name = "provento")
public class Provento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "acao_id", nullable = false)
    private Acao acao;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 15)
    private TipoProvento tipo;

    @Column(name = "valor_total", nullable = false, precision = 19, scale = 4)
    private BigDecimal valorTotal;

    @Column(name = "data_pagamento", nullable = false)
    private LocalDate dataPagamento;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    protected Provento() {
        // uso exclusivo do JPA
    }

    public Provento(Long usuarioId, Acao acao, TipoProvento tipo, BigDecimal valorTotal,
                     LocalDate dataPagamento, OffsetDateTime criadoEm) {
        this.usuarioId = usuarioId;
        this.acao = acao;
        this.tipo = tipo;
        this.valorTotal = valorTotal;
        this.dataPagamento = dataPagamento;
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

    public TipoProvento getTipo() {
        return tipo;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public LocalDate getDataPagamento() {
        return dataPagamento;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }
}