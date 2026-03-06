-- Insert admin user (Shadan Hussain). Omit user_id so it is auto-generated.
INSERT INTO users (name, email, username, password_hash, role, created_at)
VALUES (
  'Shadan Hussain',
  'shadanhussain10@gmail.com',
  'shadan',
  '$2a$10$ipA/9JZP9ApZCb9X3zEMwOGLaMqhW3yKiBZcekSQUosTWvumARZna',
  'ADMIN',
  NOW()
)
