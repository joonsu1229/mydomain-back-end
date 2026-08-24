CREATE TABLE dns_records (
    id              BIGSERIAL PRIMARY KEY,
    domain_id       BIGINT       NOT NULL REFERENCES domains(id) ON DELETE CASCADE,
    record_type     VARCHAR(10)  NOT NULL,  -- A, AAAA, CNAME, MX, TXT, NS, SRV
    name            VARCHAR(255) NOT NULL,  -- subdomain prefix or @ for root
    content         VARCHAR(500) NOT NULL,  -- value (IP, hostname, text)
    ttl             INT          NOT NULL DEFAULT 3600,
    priority        INT,                    -- MX/SRV priority
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_dns_domain_id ON dns_records(domain_id);
CREATE INDEX idx_dns_type ON dns_records(record_type);
