INSERT INTO users (id, email, password_hash, name, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'admin@clickmaster.io',
    '$2b$10$e.GbzJrgQZK9Lu5PNKgHiuRCXvU5joMGvEY.vA2f2f3VW0yKEWk.u',
    'Admin',
    NOW(),
    NOW()
)
ON CONFLICT (email) DO NOTHING;
