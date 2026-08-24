CREATE TABLE privacy_profiles (
    id              BIGSERIAL PRIMARY KEY,
    domain_id       BIGINT       NOT NULL UNIQUE REFERENCES domains(id),
    proxy_email     VARCHAR(255),
    proxy_phone     VARCHAR(20),
    enabled_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
