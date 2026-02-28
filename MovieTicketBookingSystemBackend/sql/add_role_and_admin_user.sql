-- Add role column to users table (run once; safe to re-run only if column does not exist)
ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(20) NOT NULL DEFAULT 'USER';

-- Insert admin user (Shadan Hussain). Omit user_id so it is auto-generated.
INSERT INTO users (name, email, username, password_hash, role, created_at)
VALUES (
  'Shadan Hussain',
  'example@gmail.com',
  'shadan1',
  '$2a$10$ipA/9JZP9ApZCb9X3zEMwOGLaMqhW3yKiBZcekSQUosTWvumARZna',
  'ADMIN',
  NOW()
)
ON CONFLICT (username) DO NOTHING;
