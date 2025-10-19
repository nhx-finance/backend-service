ALTER TABLE users DROP CONSTRAINT uc_users_phone_number;
ALTER TABLE users ALTER COLUMN phone_number DROP NOT NULL;
