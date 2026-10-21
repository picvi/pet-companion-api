CREATE TABLE pet_check (
    id UUID PRIMARY KEY,
    pet_id UUID NOT NULL REFERENCES pets(id) ON DELETE CASCADE,
    client_id UUID NOT NULL,
    occurred_on DATE NOT NULL,
    minute_of_day SMALLINT NOT NULL,
    source VARCHAR(16) NOT NULL,
    item VARCHAR(32) NOT NULL,
    category VARCHAR(16) NOT NULL,
    flags VARCHAR(255) NOT NULL DEFAULT '',
    answers JSONB NOT NULL DEFAULT '{}',
    substance VARCHAR(24),
    note VARCHAR(500),
    context JSONB NOT NULL,
    level VARCHAR(12) NOT NULL,
    rule_id VARCHAR(64) NOT NULL,
    rulebook VARCHAR(16) NOT NULL,
    result JSONB NOT NULL,
    status VARCHAR(12) NOT NULL,
    check_back_due_at TIMESTAMPTZ,
    resolved_on DATE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_pet_check_client UNIQUE (pet_id, client_id),
    CONSTRAINT ck_pet_check_minute CHECK (minute_of_day BETWEEN 0 AND 1439),
    CONSTRAINT ck_pet_check_source CHECK (source IN ('HOME', 'SYMPTOM_LOG', 'TIMELINE', 'FOOD', 'HEALTH')),
    CONSTRAINT ck_pet_check_level CHECK (level IN ('COMMON', 'WATCH', 'CALL_TODAY', 'GO_NOW')),
    CONSTRAINT ck_pet_check_status CHECK (status IN ('CLOSED', 'WATCHING', 'RESOLVED', 'ESCALATED', 'DISMISSED')),
    CONSTRAINT ck_pet_check_note_size CHECK (note IS NULL OR length(note) <= 500)
);

CREATE INDEX idx_pet_check_pet_day ON pet_check (pet_id, occurred_on DESC, minute_of_day DESC);
CREATE INDEX idx_pet_check_watching ON pet_check (pet_id) WHERE status = 'WATCHING';

CREATE TABLE pet_check_follow_up (
    id UUID PRIMARY KEY,
    check_id UUID NOT NULL REFERENCES pet_check(id) ON DELETE CASCADE,
    client_id UUID NOT NULL,
    answer VARCHAR(8) NOT NULL,
    level_after VARCHAR(12) NOT NULL,
    answered_on DATE NOT NULL,
    minute_of_day SMALLINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_pet_check_follow_up_client UNIQUE (check_id, client_id),
    CONSTRAINT ck_pet_check_follow_up_answer CHECK (answer IN ('BETTER', 'SAME', 'WORSE')),
    CONSTRAINT ck_pet_check_follow_up_level CHECK (level_after IN ('COMMON', 'WATCH', 'CALL_TODAY', 'GO_NOW')),
    CONSTRAINT ck_pet_check_follow_up_minute CHECK (minute_of_day BETWEEN 0 AND 1439)
);
