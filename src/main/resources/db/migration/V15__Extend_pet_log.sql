ALTER TABLE pet_log
    DROP CONSTRAINT ck_pet_log_type,
    DROP CONSTRAINT ck_pet_log_detail,
    DROP CONSTRAINT ck_pet_log_source,
    ALTER COLUMN type TYPE VARCHAR(12),
    ALTER COLUMN detail TYPE VARCHAR(20),
    ALTER COLUMN source TYPE VARCHAR(14),
    ADD COLUMN occurred_at TIMESTAMPTZ,
    ADD COLUMN utc_offset_minutes SMALLINT,
    ADD COLUMN accident BOOLEAN,
    ADD COLUMN stool_score SMALLINT,
    ADD COLUMN wellbeing VARCHAR(12),
    ADD COLUMN weight_grams INT,
    ADD COLUMN duration_minutes INT,
    ADD COLUMN label VARCHAR(80),
    ADD COLUMN dose VARCHAR(40),
    ADD COLUMN note VARCHAR(500),
    ADD COLUMN updated_at TIMESTAMP;

DELETE FROM pet_log WHERE detail = 'ACCIDENT';

UPDATE pet_log
SET occurred_at = (local_date + make_interval(mins => minute_of_day)) AT TIME ZONE 'Europe/Warsaw';

UPDATE pet_log
SET utc_offset_minutes = EXTRACT(EPOCH FROM ((local_date + make_interval(mins => minute_of_day)) - (occurred_at AT TIME ZONE 'UTC'))) / 60;

ALTER TABLE pet_log
    ALTER COLUMN occurred_at SET NOT NULL,
    ALTER COLUMN utc_offset_minutes SET NOT NULL,
    ADD CONSTRAINT ck_pet_log_type CHECK (type IN ('TOILET', 'MEAL', 'SLEEP', 'WAKE', 'WALK', 'WEIGHT', 'SYMPTOM', 'MEDICINE', 'NOTE')),
    ADD CONSTRAINT ck_pet_log_source CHECK (source IN ('NOW_CARD', 'QUICK_LOG', 'QUICK_REPEAT', 'DAY_VIEW', 'WEIGH_IN')),
    ADD CONSTRAINT ck_pet_log_offset CHECK (utc_offset_minutes BETWEEN -720 AND 840),
    ADD CONSTRAINT ck_pet_log_stool CHECK (stool_score IS NULL OR stool_score BETWEEN 1 AND 7),
    ADD CONSTRAINT ck_pet_log_wellbeing CHECK (wellbeing IS NULL OR wellbeing IN ('USUAL', 'QUIETER', 'VERY_UNWELL')),
    ADD CONSTRAINT ck_pet_log_weight CHECK (weight_grams IS NULL OR weight_grams BETWEEN 50 AND 120000),
    ADD CONSTRAINT ck_pet_log_duration CHECK (duration_minutes IS NULL OR duration_minutes BETWEEN 1 AND 600);

CREATE TABLE pet_log_flag (
    log_id UUID NOT NULL REFERENCES pet_log(id) ON DELETE CASCADE,
    flag VARCHAR(12) NOT NULL,
    PRIMARY KEY (log_id, flag),
    CONSTRAINT ck_pet_log_flag CHECK (flag IN ('BLOOD', 'MUCUS', 'WORMS', 'BLACK', 'STRAINING', 'NEW_FOOD'))
);

CREATE TABLE pet_log_change (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    log_id UUID NOT NULL,
    pet_id UUID NOT NULL REFERENCES pets(id) ON DELETE CASCADE,
    change VARCHAR(8) NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_pet_log_change CHECK (change IN ('CREATED', 'UPDATED', 'DELETED'))
);

CREATE INDEX idx_pet_log_pet_type_date ON pet_log (pet_id, type, local_date DESC);
CREATE INDEX idx_pet_log_change_pet ON pet_log_change (pet_id);
