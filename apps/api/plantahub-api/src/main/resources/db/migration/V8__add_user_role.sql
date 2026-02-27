ALTER TABLE app_user
  ADD COLUMN IF NOT EXISTS role VARCHAR(20) NOT NULL DEFAULT 'USER';

UPDATE app_user
SET role = 'ADMIN'
WHERE email = 'test@plantahub.com';