ALTER TABLE platform_domains ADD COLUMN IF NOT EXISTS verification_token VARCHAR(64);
ALTER TABLE platform_domains ADD COLUMN IF NOT EXISTS verified_at TIMESTAMPTZ;
UPDATE platform_domains SET status = 'ACTIVE' WHERE status = 'ACTIVE';
