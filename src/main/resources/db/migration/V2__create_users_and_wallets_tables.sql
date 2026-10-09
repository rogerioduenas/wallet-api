ALTER TABLE users
    DROP COLUMN first_name,
    DROP COLUMN last_name,
    ADD COLUMN full_name VARCHAR(150) NOT NULL,
    ADD COLUMN email VARCHAR(150) NOT NULL,
    ADD COLUMN password VARCHAR(255) NOT NULL,
    ADD CONSTRAINT uk_users_email UNIQUE (email);

CREATE TABLE wallets (
    id UUID NOT NULL,
    balance NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    user_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_wallets PRIMARY KEY (id),
    CONSTRAINT uk_wallets_user_id UNIQUE (user_id),
    CONSTRAINT fk_wallets_on_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT
);