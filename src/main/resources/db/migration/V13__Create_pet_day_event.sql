CREATE TABLE pet_day_event (
    id UUID PRIMARY KEY,
    pet_id UUID NOT NULL REFERENCES pets(id) ON DELETE CASCADE,
    type VARCHAR(10) NOT NULL,
    title VARCHAR(60),
    local_date DATE NOT NULL,
    start_minute INT NOT NULL,
    duration_minutes INT NOT NULL,
    recurrence VARCHAR(10) NOT NULL,
    plan_around BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT ck_pet_day_event_type CHECK (type IN ('VET', 'GROOMING', 'CLASS', 'GUESTS', 'CAR_TRIP', 'OTHER')),
    CONSTRAINT ck_pet_day_event_recurrence CHECK (recurrence IN ('NONE', 'WEEKLY')),
    CONSTRAINT ck_pet_day_event_start CHECK (start_minute BETWEEN 0 AND 1439),
    CONSTRAINT ck_pet_day_event_duration CHECK (duration_minutes BETWEEN 15 AND 720)
);

CREATE INDEX idx_pet_day_event_pet_date ON pet_day_event (pet_id, local_date);

CREATE TABLE pet_day_event_exception (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL REFERENCES pet_day_event(id) ON DELETE CASCADE,
    local_date DATE NOT NULL,
    skipped BOOLEAN NOT NULL,
    start_minute INT,
    CONSTRAINT uq_pet_day_event_exception UNIQUE (event_id, local_date),
    CONSTRAINT ck_pet_day_event_exception CHECK (skipped OR start_minute BETWEEN 0 AND 1439)
);
