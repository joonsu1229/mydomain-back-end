CREATE TABLE registrar_jobs (
    id              BIGSERIAL PRIMARY KEY,
    domain_id       BIGINT       NOT NULL REFERENCES domains(id),
    job_type        VARCHAR(30)  NOT NULL,
    payload         JSONB        NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    attempts        INT          NOT NULL DEFAULT 0,
    max_attempts    INT          NOT NULL DEFAULT 5,
    last_error      TEXT,
    next_retry_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_rj_status ON registrar_jobs(status, next_retry_at);
CREATE INDEX idx_rj_domain_id ON registrar_jobs(domain_id);
