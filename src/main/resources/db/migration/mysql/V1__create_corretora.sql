CREATE TABLE corretora (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cnpj VARCHAR(14) NOT NULL,
    razao_social VARCHAR(255) NOT NULL,
    nome_fantasia VARCHAR(255),
    email VARCHAR(255),
    telefone VARCHAR(20),
    cep VARCHAR(8) NOT NULL,
    logradouro VARCHAR(255) NOT NULL,
    numero VARCHAR(20),
    complemento VARCHAR(100),
    bairro VARCHAR(120) NOT NULL,
    cidade VARCHAR(120) NOT NULL,
    uf VARCHAR(2) NOT NULL,
    situacao_cadastral VARCHAR(50) NOT NULL,
    validada_cvm BOOLEAN NOT NULL,
    criado_em TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_corretora_cnpj UNIQUE (cnpj)
) ENGINE=InnoDB;
