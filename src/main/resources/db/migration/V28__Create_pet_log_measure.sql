CREATE TABLE pet_log_measure (
    log_id UUID NOT NULL REFERENCES pet_log (id) ON DELETE CASCADE,
    kind   VARCHAR(24) NOT NULL,
    value  INT NOT NULL,
    PRIMARY KEY (log_id, kind),
    CONSTRAINT ck_pet_log_measure_value CHECK (value > 0)
);

INSERT INTO pet_log_measure (log_id, kind, value)
SELECT id, 'STOOL_SCORE', stool_score FROM pet_log WHERE stool_score IS NOT NULL
UNION ALL
SELECT id, 'WEIGHT_GRAMS', weight_grams FROM pet_log WHERE weight_grams IS NOT NULL
UNION ALL
SELECT id, 'DURATION_MINUTES', duration_minutes FROM pet_log WHERE duration_minutes IS NOT NULL
UNION ALL
SELECT id, 'AMOUNT_GRAMS', amount_grams FROM pet_log WHERE amount_grams IS NOT NULL;

ALTER TABLE pet_log
    DROP CONSTRAINT ck_pet_log_source,
    DROP CONSTRAINT ck_pet_log_stool,
    DROP CONSTRAINT ck_pet_log_weight,
    DROP CONSTRAINT ck_pet_log_duration,
    DROP CONSTRAINT ck_pet_log_amount,
    DROP COLUMN source,
    DROP COLUMN stool_score,
    DROP COLUMN weight_grams,
    DROP COLUMN duration_minutes,
    DROP COLUMN amount_grams;
