-- Run this in Supabase SQL Editor if user creation returns 500 errors.
-- Safe to run more than once (uses IF NOT EXISTS / conditional alters).

ALTER TABLE app_user ALTER COLUMN google_id DROP NOT NULL;

ALTER TABLE app_user ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255);
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS auth_provider VARCHAR(32);
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ;

UPDATE app_user
SET auth_provider = 'GOOGLE'
WHERE auth_provider IS NULL;

UPDATE app_user
SET updated_at = COALESCE(updated_at, created_at, NOW())
WHERE updated_at IS NULL;

UPDATE app_user
SET name = COALESCE(NULLIF(name, ''), split_part(email, '@', 1))
WHERE name IS NULL OR name = '';

ALTER TABLE app_user ALTER COLUMN auth_provider SET NOT NULL;
ALTER TABLE app_user ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE journal_entry ADD COLUMN IF NOT EXISTS user_id BIGINT;
ALTER TABLE journal_entry ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ;
ALTER TABLE journal_entry ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ;
