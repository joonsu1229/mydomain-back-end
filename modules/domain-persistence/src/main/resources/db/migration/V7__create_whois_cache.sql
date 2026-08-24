CREATE TABLE whois_cache (
    id              BIGSERIAL PRIMARY KEY,
    domain_name     VARCHAR(255) NOT NULL UNIQUE,
    raw_response    JSONB        NOT NULL,
    cached_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_whois_expires ON whois_cache(expires_at);
