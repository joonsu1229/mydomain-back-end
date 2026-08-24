-- V13: Platform Domains — root domains owned by the platform
-- Users create subdomains under these platform domains.

CREATE TABLE platform_domains (
    id              BIGSERIAL PRIMARY KEY,
    name_unicode    VARCHAR(255) NOT NULL,
    name_punycode   VARCHAR(255) NOT NULL,
    display_name    VARCHAR(100) NOT NULL,
    description     VARCHAR(500),
    status          VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE',
    registrar_ref   VARCHAR(100),
    ns_default      VARCHAR(255),
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_platform_domains_punycode ON platform_domains(name_punycode);
CREATE INDEX idx_platform_domains_active ON platform_domains(is_active);

-- Add reference from domains (subdomains) to their platform domain
ALTER TABLE domains ADD COLUMN platform_domain_id BIGINT REFERENCES platform_domains(id);
CREATE INDEX idx_domains_platform_domain_id ON domains(platform_domain_id);

-- Seed default platform domain (backward compatible with hardcoded 'yourhost.kr')
INSERT INTO platform_domains (name_unicode, name_punycode, display_name, description, status, is_active)
VALUES ('yourhost.kr', 'yourhost.kr', 'YourHost', '기본 플랫폼 도메인', 'ACTIVE', TRUE);

-- Update existing domains to reference the seed platform domain
UPDATE domains SET platform_domain_id = (SELECT id FROM platform_domains WHERE name_unicode = 'yourhost.kr');
