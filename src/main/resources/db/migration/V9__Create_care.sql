CREATE TABLE care_item (
    code VARCHAR(40) PRIMARY KEY,
    category VARCHAR(20) NOT NULL,
    requirement VARCHAR(20) NOT NULL,
    interval_editable BOOLEAN NOT NULL DEFAULT FALSE,
    drives_vaccination_status BOOLEAN NOT NULL DEFAULT FALSE,
    legal_min_age_months INT,
    legal_window_days INT,
    legal_repeat_months INT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL,
    CONSTRAINT ck_care_item_category CHECK (category IN ('VACCINE', 'PARASITE')),
    CONSTRAINT ck_care_item_requirement CHECK (requirement IN ('REQUIRED', 'RECOMMENDED', 'OPTIONAL'))
);

CREATE TABLE care_protocol_step (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    item_code VARCHAR(40) NOT NULL REFERENCES care_item(code),
    step_order INT NOT NULL,
    kind VARCHAR(20) NOT NULL,
    min_age_weeks INT,
    max_age_weeks INT,
    gap_days INT,
    season_start_month INT,
    season_end_month INT,
    CONSTRAINT uq_care_step_item_order UNIQUE (item_code, step_order),
    CONSTRAINT ck_care_step_kind CHECK (kind IN ('SERIES', 'BOOSTER', 'REPEAT')),
    CONSTRAINT ck_care_step_season CHECK ((season_start_month IS NULL) = (season_end_month IS NULL))
);

CREATE TABLE care_event (
    id UUID PRIMARY KEY,
    pet_id UUID NOT NULL REFERENCES pets(id) ON DELETE CASCADE,
    item_code VARCHAR(40) NOT NULL REFERENCES care_item(code),
    performed_on DATE NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_care_event_pet_item ON care_event (pet_id, item_code, performed_on DESC);

CREATE TABLE pet_care_setting (
    id UUID PRIMARY KEY,
    pet_id UUID NOT NULL REFERENCES pets(id) ON DELETE CASCADE,
    item_code VARCHAR(40) NOT NULL REFERENCES care_item(code),
    reminders_enabled BOOLEAN NOT NULL,
    paused_until DATE,
    interval_days_override INT,
    CONSTRAINT uq_pet_care_setting UNIQUE (pet_id, item_code),
    CONSTRAINT ck_pet_care_setting_interval CHECK (interval_days_override IS NULL OR interval_days_override BETWEEN 7 AND 365)
);
