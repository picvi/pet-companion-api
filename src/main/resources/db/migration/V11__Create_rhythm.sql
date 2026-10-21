CREATE TABLE rhythm_rule (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    from_weeks INT NOT NULL,
    to_weeks INT NOT NULL,
    size_category VARCHAR(10),
    awake_min INT NOT NULL,
    awake_max INT NOT NULL,
    awake_standard INT NOT NULL,
    nap_min INT NOT NULL,
    nap_max INT NOT NULL,
    nap_standard INT NOT NULL,
    meals_min INT NOT NULL,
    meals_max INT NOT NULL,
    meals_standard INT NOT NULL,
    toilet_interval_minutes INT NOT NULL,
    night_trips INT NOT NULL,
    meal_safety_note BOOLEAN NOT NULL,
    soft_ground_play BOOLEAN NOT NULL,
    source_codes VARCHAR(200) NOT NULL,
    CONSTRAINT ck_rhythm_rule_band CHECK (from_weeks < to_weeks),
    CONSTRAINT ck_rhythm_rule_size CHECK (size_category IS NULL OR size_category IN ('SMALL', 'MEDIUM', 'LARGE', 'GIANT'))
);

CREATE UNIQUE INDEX uq_rhythm_rule_sized ON rhythm_rule (from_weeks, size_category) WHERE size_category IS NOT NULL;
CREATE UNIQUE INDEX uq_rhythm_rule_default ON rhythm_rule (from_weeks) WHERE size_category IS NULL;

CREATE TABLE pet_rhythm_settings (
    id UUID PRIMARY KEY,
    pet_id UUID NOT NULL UNIQUE REFERENCES pets(id) ON DELETE CASCADE,
    wake_minute INT NOT NULL,
    bed_minute INT NOT NULL,
    custom_awake_minutes INT,
    meal_amount_grams INT,
    CONSTRAINT ck_pet_rhythm_day CHECK (wake_minute >= 0 AND bed_minute <= 1439 AND wake_minute < bed_minute),
    CONSTRAINT ck_pet_rhythm_meal_amount CHECK (meal_amount_grams IS NULL OR meal_amount_grams BETWEEN 1 AND 2000)
);

CREATE TABLE pet_meal_time (
    settings_id UUID NOT NULL REFERENCES pet_rhythm_settings(id) ON DELETE CASCADE,
    meal_minute INT NOT NULL,
    PRIMARY KEY (settings_id, meal_minute)
);
