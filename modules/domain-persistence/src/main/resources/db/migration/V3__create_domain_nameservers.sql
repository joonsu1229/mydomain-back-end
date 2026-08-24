CREATE TABLE domain_nameservers (
    id              BIGSERIAL PRIMARY KEY,
    domain_id       BIGINT       NOT NULL REFERENCES domains(id) ON DELETE CASCADE,
    host            VARCHAR(255) NOT NULL,
    ip              VARCHAR(45),
    sort_order      SMALLINT     NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_dns_domain_id ON domain_nameservers(domain_id);
