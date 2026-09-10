-- Run this in Supabase SQL Editor if auth or journal APIs return 500 errors.
-- Safe to run more than once (uses IF NOT EXISTS / conditional updates).

-- app_user: support email/password accounts
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

-- journal_entry: user ownership + timestamps
ALTER TABLE journal_entry ADD COLUMN IF NOT EXISTS user_id BIGINT;
ALTER TABLE journal_entry ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ;
ALTER TABLE journal_entry ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ;

UPDATE journal_entry
SET user_id = (SELECT id FROM app_user ORDER BY id ASC LIMIT 1)
WHERE user_id IS NULL
  AND EXISTS (SELECT 1 FROM app_user);

UPDATE journal_entry
SET created_at = COALESCE(created_at, NOW()),
    updated_at = COALESCE(updated_at, NOW())
WHERE created_at IS NULL OR updated_at IS NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM journal_entry WHERE user_id IS NULL) THEN
        ALTER TABLE journal_entry ALTER COLUMN user_id SET NOT NULL;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM journal_entry WHERE created_at IS NULL) THEN
        ALTER TABLE journal_entry ALTER COLUMN created_at SET NOT NULL;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM journal_entry WHERE updated_at IS NULL) THEN
        ALTER TABLE journal_entry ALTER COLUMN updated_at SET NOT NULL;
    END IF;
END $$;

ALTER TABLE journal_entry DROP CONSTRAINT IF EXISTS fk_journal_entry_user;
ALTER TABLE journal_entry
    ADD CONSTRAINT fk_journal_entry_user
    FOREIGN KEY (user_id) REFERENCES app_user(id);
