-- The admin is set by SQL: UPDATE users SET role = 'ADMIN' WHERE email = '...'. It takes effect with the next access token.
ALTER TABLE users
    ADD COLUMN role VARCHAR(5) NOT NULL DEFAULT 'USER',
    ADD CONSTRAINT ck_users_role CHECK (role IN ('USER', 'ADMIN'));

-- Locks for scheduled jobs, so a second server never runs the same job twice (ShedLock).
CREATE TABLE IF NOT EXISTS shedlock (
    name VARCHAR(64) NOT NULL PRIMARY KEY,
    lock_until TIMESTAMP NOT NULL,
    locked_at TIMESTAMP NOT NULL,
    locked_by VARCHAR(255) NOT NULL
);
