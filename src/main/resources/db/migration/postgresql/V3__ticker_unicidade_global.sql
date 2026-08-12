ALTER TABLE acao DROP CONSTRAINT uk_acao_ticker_mercado;
ALTER TABLE acao ADD CONSTRAINT uk_acao_ticker UNIQUE (ticker);