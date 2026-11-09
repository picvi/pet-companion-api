CREATE TABLE medication_course (
    id                UUID PRIMARY KEY,
    pet_id            UUID        NOT NULL REFERENCES pets (id) ON DELETE CASCADE,
    client_id         UUID        NOT NULL,
    name              VARCHAR(80) NOT NULL,
    amount_hundredths INT         NOT NULL,
    unit              VARCHAR(16) NOT NULL,
    schedule          VARCHAR(16) NOT NULL,
    every_days        INT,
    with_food         VARCHAR(8)  NOT NULL,
    starts_on         DATE        NOT NULL,
    start_minute      INT         NOT NULL,
    ends_on           DATE,
    ends_minute       INT,
    max_per_day       INT,
    stopped_on        DATE,
    stopped_minute    INT,
    reminders         BOOLEAN     NOT NULL DEFAULT TRUE,
    version           BIGINT      NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ NOT NULL,
    updated_at        TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_medication_course_client UNIQUE (pet_id, client_id),
    CONSTRAINT uq_medication_course_id_pet UNIQUE (id, pet_id),
    CONSTRAINT ck_medication_course_amount CHECK (amount_hundredths BETWEEN 1 AND 100000),
    CONSTRAINT ck_medication_course_unit CHECK (unit IN ('TABLET', 'CAPSULE', 'ML', 'DROPS', 'SACHET', 'APPLICATION', 'WASH')),
    CONSTRAINT ck_medication_course_schedule CHECK (schedule IN ('DAILY', 'EVERY_N_DAYS', 'AS_NEEDED')),
    CONSTRAINT ck_medication_course_every CHECK ((schedule = 'EVERY_N_DAYS') = (every_days IS NOT NULL) AND (every_days IS NULL OR every_days BETWEEN 2 AND 14)),
    CONSTRAINT ck_medication_course_max CHECK (max_per_day IS NULL OR (schedule = 'AS_NEEDED' AND max_per_day BETWEEN 1 AND 6)),
    CONSTRAINT ck_medication_course_with_food CHECK (with_food IN ('YES', 'NO', 'ANY')),
    CONSTRAINT ck_medication_course_start_minute CHECK (start_minute BETWEEN 0 AND 1439),
    CONSTRAINT ck_medication_course_end CHECK ((ends_on IS NULL) = (ends_minute IS NULL) AND (ends_minute IS NULL OR ends_minute BETWEEN 0 AND 1439)),
    CONSTRAINT ck_medication_course_stop CHECK ((stopped_on IS NULL) = (stopped_minute IS NULL) AND (stopped_minute IS NULL OR stopped_minute BETWEEN 0 AND 1439))
);

CREATE INDEX idx_medication_course_pet ON medication_course (pet_id, starts_on DESC);

CREATE TABLE medication_dose_time (
    course_id     UUID NOT NULL REFERENCES medication_course (id) ON DELETE CASCADE,
    minute_of_day INT  NOT NULL,
    PRIMARY KEY (course_id, minute_of_day),
    CONSTRAINT ck_medication_dose_time_minute CHECK (minute_of_day BETWEEN 0 AND 1439)
);

ALTER TABLE pet_log
    ADD COLUMN course_id   UUID,
    ADD COLUMN dose_on     DATE,
    ADD COLUMN dose_minute INT,
    ADD CONSTRAINT ck_pet_log_course_dose CHECK (
        (course_id IS NULL AND dose_on IS NULL AND dose_minute IS NULL)
        OR (course_id IS NOT NULL AND type = 'MEDICINE'
            AND (dose_on IS NULL) = (dose_minute IS NULL)
            AND (dose_minute IS NULL OR dose_minute BETWEEN 0 AND 1439))),
    ADD CONSTRAINT fk_pet_log_course FOREIGN KEY (course_id, pet_id)
        REFERENCES medication_course (id, pet_id) ON DELETE SET NULL (course_id, dose_on, dose_minute);

CREATE UNIQUE INDEX uq_pet_log_course_dose ON pet_log (course_id, dose_on, dose_minute) WHERE dose_on IS NOT NULL;
CREATE INDEX idx_pet_log_course_date ON pet_log (course_id, local_date, minute_of_day) WHERE course_id IS NOT NULL;
