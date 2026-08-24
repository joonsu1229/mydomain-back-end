CREATE TABLE audit_logs (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT,
    domain_id       BIGINT,
    action          VARCHAR(50)  NOT NULL,
    details         JSONB,
    ip_address      VARCHAR(45),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_domain_id ON audit_logs(domain_id);
CREATE INDEX idx_audit_created_at ON audit_logs(created_at);
