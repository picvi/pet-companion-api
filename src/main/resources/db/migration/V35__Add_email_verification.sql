-- Accounts that existed before verification was introduced are treated as verified; new registrations start unverified.
ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE users ALTER COLUMN email_verified SET DEFAULT FALSE;

-- Emails are matched case-insensitively. Fails if two accounts differ only by case; merge or rename one first.
UPDATE users SET email = lower(trim(email)) WHERE email <> lower(trim(email));
ALTER TABLE users ADD CONSTRAINT users_email_lowercase CHECK (email = lower(email));

-- One code per user at a time, reissued in place. Only its hash is stored; wrong guesses are counted,
-- and the send counters survive a reissue so resends can be limited (cooldown and a cap per window).
CREATE TABLE email_verification_codes (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    code_hash VARCHAR(64) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_sent_at TIMESTAMP WITH TIME ZONE NOT NULL,
    window_started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    sends_in_window INT NOT NULL DEFAULT 1
);
