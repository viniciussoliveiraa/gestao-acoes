CREATE TABLE acao (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticker VARCHAR(20) NOT NULL,
    nome_empresa VARCHAR(255),
    mercado VARCHAR(20) NOT NULL,
    moeda VARCHAR(3) NOT NULL,
    cotacao_atual DECIMAL(19,4) NOT NULL,
    data_hora_cotacao TIMESTAMP(6) NOT NULL,
    provedor_origem VARCHAR(30) NOT NULL,
    criado_em TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_acao_ticker UNIQUE (ticker)
) ENGINE=InnoDB;
