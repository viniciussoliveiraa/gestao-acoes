CREATE TABLE acao (
    id BIGSERIAL PRIMARY KEY,
    ticker VARCHAR(20) NOT NULL,
    nome_empresa VARCHAR(255),
    mercado VARCHAR(20) NOT NULL,
    moeda VARCHAR(3) NOT NULL,
    cotacao_atual NUMERIC(19,4) NOT NULL,
    data_hora_cotacao TIMESTAMP WITH TIME ZONE NOT NULL,
    provedor_origem VARCHAR(30) NOT NULL,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_acao_ticker_mercado UNIQUE (ticker, mercado)
);