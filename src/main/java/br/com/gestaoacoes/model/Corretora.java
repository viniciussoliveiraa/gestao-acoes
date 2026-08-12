package br.com.gestaoacoes.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "corretora")
public class Corretora {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 14)
    private String cnpj;

    @Column(name = "razao_social", nullable = false)
    private String razaoSocial;

    @Column(name = "nome_fantasia")
    private String nomeFantasia;

    private String email;

    private String telefone;

    @Column(nullable = false, length = 8)
    private String cep;

    @Column(nullable = false)
    private String logradouro;

    private String numero;

    private String complemento;

    @Column(nullable = false)
    private String bairro;

    @Column(nullable = false)
    private String cidade;

    @Column(nullable = false, length = 2)
    private String uf;

    @Column(name = "situacao_cadastral", nullable = false)
    private String situacaoCadastral;

    @Column(name = "validada_cvm", nullable = false)
    private boolean validadaCvm;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    protected Corretora() {
        // uso exclusivo do JPA
    }

    public Corretora(String cnpj, String razaoSocial, String nomeFantasia, String email, String telefone,
                      String cep, String logradouro, String numero, String complemento, String bairro,
                      String cidade, String uf, String situacaoCadastral, boolean validadaCvm,
                      OffsetDateTime criadoEm) {
        this.cnpj = cnpj;
        this.razaoSocial = razaoSocial;
        this.nomeFantasia = nomeFantasia;
        this.email = email;
        this.telefone = telefone;
        this.cep = cep;
        this.logradouro = logradouro;
        this.numero = numero;
        this.complemento = complemento;
        this.bairro = bairro;
        this.cidade = cidade;
        this.uf = uf;
        this.situacaoCadastral = situacaoCadastral;
        this.validadaCvm = validadaCvm;
        this.criadoEm = criadoEm;
    }

    public Long getId() {
        return id;
    }

    public String getCnpj() {
        return cnpj;
    }

    public String getRazaoSocial() {
        return razaoSocial;
    }

    public String getNomeFantasia() {
        return nomeFantasia;
    }

    public String getEmail() {
        return email;
    }

    public String getTelefone() {
        return telefone;
    }

    public String getCep() {
        return cep;
    }

    public String getLogradouro() {
        return logradouro;
    }

    public String getNumero() {
        return numero;
    }

    public String getComplemento() {
        return complemento;
    }

    public String getBairro() {
        return bairro;
    }

    public String getCidade() {
        return cidade;
    }

    public String getUf() {
        return uf;
    }

    public String getSituacaoCadastral() {
        return situacaoCadastral;
    }

    public boolean isValidadaCvm() {
        return validadaCvm;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }
}