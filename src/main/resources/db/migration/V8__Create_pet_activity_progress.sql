CREATE TABLE pet_activity_progress (
    id UUID PRIMARY KEY,
    pet_id UUID NOT NULL REFERENCES pets(id) ON DELETE CASCADE,
    activity_id BIGINT NOT NULL REFERENCES development_activity(id),
    status VARCHAR(20) NOT NULL,
    last_outcome VARCHAR(20),
    attempt_days INT NOT NULL DEFAULT 0,
    first_attempt_on DATE,
    last_attempt_on DATE,
    completed_on DATE,
    snoozed_until DATE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_progress_pet_activity UNIQUE (pet_id, activity_id),
    CONSTRAINT ck_progress_status CHECK (status IN ('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED', 'NOT_POSSIBLE')),
    CONSTRAINT ck_progress_outcome CHECK (last_outcome IS NULL OR last_outcome IN ('CALM', 'UNSURE', 'SCARED', 'PRACTISED', 'LEARNED')),
    CONSTRAINT ck_progress_completed CHECK ((status = 'COMPLETED') = (completed_on IS NOT NULL))
);
