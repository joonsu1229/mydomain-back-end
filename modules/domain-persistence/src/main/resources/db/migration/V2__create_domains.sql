CREATE TABLE domains (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES users(id),
    name_unicode    VARCHAR(255) NOT NULL,
    name_punycode   VARCHAR(255) NOT NULL,
    tld             VARCHAR(20)  NOT NULL,
    status          VARCHAR(30)  NOT NULL DEFAULT 'RESERVED',
    registrar_ref   VARCHAR(100),
    expires_at      DATE,
    privacy_enabled BOOLEAN      NOT NULL DEFAULT FALSE,
    paid_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_domains_punycode ON domains(name_punycode);
CREATE INDEX idx_domains_user_id ON domains(user_id);
CREATE INDEX idx_domains_status ON domains(status);
