CREATE TABLE orders (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES users(id),
    domain_id       BIGINT       REFERENCES domains(id),
    order_number    VARCHAR(64)  NOT NULL UNIQUE,
    amount          INT          NOT NULL,
    currency        VARCHAR(3)   NOT NULL DEFAULT 'KRW',
    product_type    VARCHAR(30)  NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_orders_number ON orders(order_number);
CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_domain_id ON orders(domain_id);

CREATE TABLE payments (
    id              BIGSERIAL PRIMARY KEY,
    order_id        BIGINT       NOT NULL REFERENCES orders(id),
    pg_provider     VARCHAR(20)  NOT NULL DEFAULT 'TOSS',
    pg_payment_key  VARCHAR(200) NOT NULL UNIQUE,
    amount          INT          NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'CONFIRMED',
    raw_payload     JSONB,
    paid_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payments_order_id ON payments(order_id);
CREATE UNIQUE INDEX idx_payments_key ON payments(pg_payment_key);
