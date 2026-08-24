-- Add username column for separate login ID (distinct from email)
ALTER TABLE users ADD COLUMN IF NOT EXISTS username VARCHAR(30);
-- Migrate existing users: use email prefix as default username
UPDATE users SET username = SPLIT_PART(email, '@', 1) WHERE username IS NULL;
-- Enforce NOT NULL and UNIQUE
ALTER TABLE users ALTER COLUMN username SET NOT NULL;
ALTER TABLE users ADD CONSTRAINT uq_users_username UNIQUE (username);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
