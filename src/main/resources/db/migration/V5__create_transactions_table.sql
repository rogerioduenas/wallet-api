CREATE TABLE transactions (
                              id UUID PRIMARY KEY,
                              wallet_id UUID NOT NULL,
                              amount NUMERIC(19, 2) NOT NULL,
                              type VARCHAR(30) NOT NULL,
                              created_at TIMESTAMP WITH TIME ZONE NOT NULL,

                              CONSTRAINT fk_transactions_wallet
                                  FOREIGN KEY (wallet_id)
                                      REFERENCES wallets(id)
);

CREATE INDEX idx_transactions_wallet_id ON transactions(wallet_id);