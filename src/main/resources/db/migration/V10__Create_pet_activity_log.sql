CREATE TABLE pet_activity_log (
    id UUID PRIMARY KEY,
    pet_id UUID NOT NULL REFERENCES pets(id) ON DELETE CASCADE,
    activity_id BIGINT NOT NULL REFERENCES development_activity(id),
    performed_on DATE NOT NULL,
    outcome VARCHAR(20) NOT NULL,
    duration_seconds INT,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT ck_log_outcome CHECK (outcome IN ('CALM', 'UNSURE', 'SCARED', 'PRACTISED', 'LEARNED')),
    CONSTRAINT ck_log_duration CHECK (duration_seconds IS NULL OR duration_seconds > 0)
);

CREATE INDEX idx_log_pet_activity_time ON pet_activity_log (pet_id, activity_id, performed_on DESC, created_at DESC);

-- Keep what the progress summary already knows: its latest attempt becomes the first log entry.
INSERT INTO pet_activity_log (id, pet_id, activity_id, performed_on, outcome, created_at)
SELECT gen_random_uuid(), pet_id, activity_id, last_attempt_on, last_outcome, updated_at
FROM pet_activity_progress
WHERE last_attempt_on IS NOT NULL AND last_outcome IS NOT NULL;
