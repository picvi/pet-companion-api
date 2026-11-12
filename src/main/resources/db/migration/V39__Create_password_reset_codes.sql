-- One reset code per user at a time, reissued in place, like email_verification_codes.
-- Only its hash is stored; wrong guesses are counted and the send counters survive a reissue.
CREATE TABLE password_reset_codes (
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
