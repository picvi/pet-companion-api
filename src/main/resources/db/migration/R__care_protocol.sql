INSERT INTO care_item
    (code, category, requirement, interval_editable, drives_vaccination_status, legal_min_age_months, legal_window_days, legal_repeat_months, active, sort_order)
VALUES
    ('RABIES', 'VACCINE', 'REQUIRED', FALSE, FALSE, 3, 30, 12, TRUE, 10),
    ('DHPPI', 'VACCINE', 'RECOMMENDED', FALSE, TRUE, NULL, NULL, NULL, TRUE, 20),
    ('LEPTOSPIROSIS', 'VACCINE', 'RECOMMENDED', FALSE, FALSE, NULL, NULL, NULL, TRUE, 30),
    ('KENNEL_COUGH', 'VACCINE', 'OPTIONAL', FALSE, FALSE, NULL, NULL, NULL, TRUE, 40),
    ('DEWORMING', 'PARASITE', 'RECOMMENDED', TRUE, FALSE, NULL, NULL, NULL, TRUE, 50),
    ('TICK_PROTECTION', 'PARASITE', 'RECOMMENDED', TRUE, FALSE, NULL, NULL, NULL, TRUE, 60)
ON CONFLICT (code) DO UPDATE SET
    category = EXCLUDED.category,
    requirement = EXCLUDED.requirement,
    interval_editable = EXCLUDED.interval_editable,
    drives_vaccination_status = EXCLUDED.drives_vaccination_status,
    legal_min_age_months = EXCLUDED.legal_min_age_months,
    legal_window_days = EXCLUDED.legal_window_days,
    legal_repeat_months = EXCLUDED.legal_repeat_months,
    active = TRUE,
    sort_order = EXCLUDED.sort_order;

UPDATE care_item
SET active = FALSE
WHERE code NOT IN ('RABIES', 'DHPPI', 'LEPTOSPIROSIS', 'KENNEL_COUGH', 'DEWORMING', 'TICK_PROTECTION');

DELETE FROM care_protocol_step;

INSERT INTO care_protocol_step
    (item_code, step_order, kind, min_age_weeks, max_age_weeks, gap_days, season_start_month, season_end_month)
VALUES
    ('RABIES', 1, 'SERIES', NULL, NULL, NULL, NULL, NULL),
    ('RABIES', 2, 'REPEAT', NULL, NULL, 365, NULL, NULL),
    ('DHPPI', 1, 'SERIES', 6, NULL, NULL, NULL, NULL),
    ('DHPPI', 2, 'SERIES', 9, NULL, 21, NULL, NULL),
    ('DHPPI', 3, 'SERIES', 16, NULL, 21, NULL, NULL),
    ('DHPPI', 4, 'BOOSTER', NULL, NULL, 365, NULL, NULL),
    ('DHPPI', 5, 'REPEAT', NULL, NULL, 1095, NULL, NULL),
    ('LEPTOSPIROSIS', 1, 'SERIES', 8, NULL, NULL, NULL, NULL),
    ('LEPTOSPIROSIS', 2, 'SERIES', 8, NULL, 21, NULL, NULL),
    ('LEPTOSPIROSIS', 3, 'REPEAT', NULL, NULL, 365, NULL, NULL),
    ('KENNEL_COUGH', 1, 'SERIES', 8, NULL, NULL, NULL, NULL),
    ('KENNEL_COUGH', 2, 'REPEAT', NULL, NULL, 365, NULL, NULL),
    ('DEWORMING', 1, 'REPEAT', 2, 26, 30, NULL, NULL),
    ('DEWORMING', 2, 'REPEAT', NULL, NULL, 90, NULL, NULL),
    ('TICK_PROTECTION', 1, 'REPEAT', 8, NULL, 28, 3, 11);
