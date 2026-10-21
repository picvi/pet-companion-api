CREATE TABLE pet_log (
    id UUID PRIMARY KEY,
    pet_id UUID NOT NULL REFERENCES pets(id) ON DELETE CASCADE,
    client_id UUID NOT NULL UNIQUE,
    type VARCHAR(10) NOT NULL,
    detail VARCHAR(12),
    amount_grams INT,
    local_date DATE NOT NULL,
    minute_of_day INT NOT NULL,
    source VARCHAR(12) NOT NULL,
    logged_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT ck_pet_log_type CHECK (type IN ('TOILET', 'MEAL', 'SLEEP', 'WAKE')),
    CONSTRAINT ck_pet_log_detail CHECK (detail IS NULL OR detail IN ('PEE', 'POO', 'BOTH', 'ACCIDENT', 'ATE_ALL', 'LEFT_SOME')),
    CONSTRAINT ck_pet_log_source CHECK (source IN ('NOW_CARD', 'QUICK_LOG', 'DAY_VIEW')),
    CONSTRAINT ck_pet_log_minute CHECK (minute_of_day BETWEEN 0 AND 1439),
    CONSTRAINT ck_pet_log_amount CHECK (amount_grams IS NULL OR amount_grams BETWEEN 1 AND 2000)
);

CREATE INDEX idx_pet_log_pet_date ON pet_log (pet_id, local_date, minute_of_day);
