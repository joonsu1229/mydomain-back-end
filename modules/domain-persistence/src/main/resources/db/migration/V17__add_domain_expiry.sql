ALTER TABLE platform_domains ADD COLUMN IF NOT EXISTS expires_at TIMESTAMPTZ;
-- Mark existing domains as expired if they were deactivated
UPDATE platform_domains SET status = 'EXPIRED' WHERE is_active = FALSE AND status != 'PENDING';
