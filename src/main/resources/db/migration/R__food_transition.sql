-- Pace tables. STANDARD is 7 days (25/50/75/100%), SLOW 10 days; IMMEDIATE is the all-at-once switch.
-- source_code is the evidence behind a row; reviewed_by stays empty until a vet has signed the table off.
INSERT INTO food_transition_pace (pace, step_order, days, new_percent, source_code, status, reviewed_by)
VALUES
    ('STANDARD', 1, 2, 25, 'RULE_OF_THUMB', 'ACTIVE', NULL),
    ('STANDARD', 2, 2, 50, 'RULE_OF_THUMB', 'ACTIVE', NULL),
    ('STANDARD', 3, 2, 75, 'RULE_OF_THUMB', 'ACTIVE', NULL),
    ('STANDARD', 4, 1, 100, 'RULE_OF_THUMB', 'ACTIVE', NULL),
    ('SLOW', 1, 3, 25, 'RULE_OF_THUMB', 'ACTIVE', NULL),
    ('SLOW', 2, 3, 50, 'RULE_OF_THUMB', 'ACTIVE', NULL),
    ('SLOW', 3, 3, 75, 'RULE_OF_THUMB', 'ACTIVE', NULL),
    ('SLOW', 4, 1, 100, 'RULE_OF_THUMB', 'ACTIVE', NULL),
    ('IMMEDIATE', 1, 1, 100, 'RULE_OF_THUMB', 'ACTIVE', NULL)
ON CONFLICT (pace, step_order) DO UPDATE SET
    days = EXCLUDED.days,
    new_percent = EXCLUDED.new_percent,
    source_code = EXCLUDED.source_code,
    status = EXCLUDED.status;

DELETE FROM food_transition_pace
WHERE (pace, step_order) NOT IN (('STANDARD', 1), ('STANDARD', 2), ('STANDARD', 3), ('STANDARD', 4),
                                 ('SLOW', 1), ('SLOW', 2), ('SLOW', 3), ('SLOW', 4), ('IMMEDIATE', 1));
