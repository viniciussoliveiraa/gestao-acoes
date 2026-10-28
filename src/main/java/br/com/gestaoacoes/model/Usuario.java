package br.com.gestaoacoes.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Papel papel;

    @Column(nullable = false)
    private Boolean ativo;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    protected Usuario() {
        // uso exclusivo do JPA
    }

    /**
     * Construtor usado pelo autocadastro público ({@code POST /auth/registrar}), que sempre cria
     * o usuário com papel {@link Papel#USER} e ativo.
     */
    public Usuario(String nome, String email, String senhaHash, OffsetDateTime criadoEm) {
        this(nome, email, senhaHash, Papel.USER, true, criadoEm);
    }

    /**
     * Construtor completo, usado pelo gerenciamento administrativo de usuários e pela criação do
     * administrador inicial, onde papel e situação são explícitos.
     */
    public Usuario(String nome, String email, String senhaHash, Papel papel, Boolean ativo, OffsetDateTime criadoEm) {
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
        this.papel = papel;
        this.ativo = ativo;
        this.criadoEm = criadoEm;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public Papel getPapel() {
        return papel;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPapel(Papel papel) {
        this.papel = papel;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    public void setSenhaHash(String senhaHash) {
        this.senhaHash = senhaHash;
    }
}
