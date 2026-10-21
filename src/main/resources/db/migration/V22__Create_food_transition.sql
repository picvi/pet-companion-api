-- Every change of main food is a row, planned or all at once (pace IMMEDIATE); that is what makes planned and
-- abrupt changes comparable later. Replace = an IMMEDIATE transition that is DONE on the day it starts.
CREATE TABLE food_transition_pace (
    pace VARCHAR(10) NOT NULL,
    step_order INT NOT NULL,
    days INT NOT NULL,
    new_percent INT NOT NULL,
    source_code VARCHAR(40) NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
    reviewed_by VARCHAR(80),
    PRIMARY KEY (pace, step_order),
    CONSTRAINT ck_food_transition_pace_pace CHECK (pace IN ('STANDARD', 'SLOW', 'IMMEDIATE')),
    CONSTRAINT ck_food_transition_pace_days CHECK (days BETWEEN 1 AND 14),
    CONSTRAINT ck_food_transition_pace_percent CHECK (new_percent BETWEEN 1 AND 100),
    CONSTRAINT ck_food_transition_pace_status CHECK (status IN ('ACTIVE', 'DRAFT'))
);

CREATE TABLE food_transition (
    id UUID PRIMARY KEY,
    pet_id UUID NOT NULL REFERENCES pets(id) ON DELETE CASCADE,
    client_id UUID NOT NULL,
    from_food_id UUID NOT NULL REFERENCES pet_food(id),
    to_food_id UUID NOT NULL REFERENCES pet_food(id),
    pace VARCHAR(10) NOT NULL,
    start_on DATE NOT NULL,
    status VARCHAR(9) NOT NULL,
    ended_on DATE,
    end_reason VARCHAR(10),
    finished_seen_at TIMESTAMPTZ,
    -- the log behind the tummy reaction the owner has already seen (see TummyReactionService)
    reaction_seen_log_id UUID,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_food_transition_client UNIQUE (pet_id, client_id),
    CONSTRAINT ck_food_transition_pace CHECK (pace IN ('STANDARD', 'SLOW', 'IMMEDIATE')),
    CONSTRAINT ck_food_transition_status CHECK (status IN ('ACTIVE', 'PAUSED', 'DONE', 'CANCELLED')),
    CONSTRAINT ck_food_transition_reason CHECK (end_reason IS NULL OR end_reason IN ('FINISHED', 'WENT_BACK', 'CANCELLED', 'REPLACED')),
    CONSTRAINT ck_food_transition_end CHECK ((status IN ('DONE', 'CANCELLED')) = (end_reason IS NOT NULL))
);

-- At most one running change per pet.
CREATE UNIQUE INDEX uq_food_transition_open ON food_transition (pet_id) WHERE status IN ('ACTIVE', 'PAUSED');
CREATE INDEX idx_food_transition_pet ON food_transition (pet_id, start_on DESC);

-- A slow-down of one step. Unique per step, so an automatic slow-down happens once per step; every one is a countable event.
-- Deleting the log that caused it (Undo) deletes the slow-down. days = 0 is a tombstone: the owner undid an automatic
-- slow-down, and the safety net on read must not add it again.
CREATE TABLE food_transition_extension (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    transition_id UUID NOT NULL REFERENCES food_transition(id) ON DELETE CASCADE,
    step_order INT NOT NULL,
    days INT NOT NULL,
    reason_log_id UUID REFERENCES pet_log(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_food_transition_extension UNIQUE (transition_id, step_order),
    CONSTRAINT ck_food_transition_extension_days CHECK (days BETWEEN 0 AND 7)
);

-- Pauses as intervals: the plan does not advance on the days paused_on <= day < resumed_on (still paused when NULL).
CREATE TABLE food_transition_pause (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    transition_id UUID NOT NULL REFERENCES food_transition(id) ON DELETE CASCADE,
    paused_on DATE NOT NULL,
    resumed_on DATE,
    CONSTRAINT ck_food_transition_pause CHECK (resumed_on IS NULL OR resumed_on >= paused_on)
);

CREATE INDEX idx_food_transition_pause_transition ON food_transition_pause (transition_id);
