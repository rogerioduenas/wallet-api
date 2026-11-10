ALTER TABLE transactions
    ADD COLUMN recipient_wallet_id UUID,
ADD CONSTRAINT fk_transactions_recipient_wallet
    FOREIGN KEY (recipient_wallet_id)
    REFERENCES wallets(id);

CREATE INDEX idx_transactions_recipient_wallet_id ON transactions(recipient_wallet_id);