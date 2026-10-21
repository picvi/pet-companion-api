ALTER TABLE pet_vet_visit ADD COLUMN next_check_event_id UUID REFERENCES pet_day_event (id) ON DELETE SET NULL;

CREATE UNIQUE INDEX uq_pet_vet_visit_next_check_event ON pet_vet_visit (next_check_event_id)
    WHERE next_check_event_id IS NOT NULL;

CREATE INDEX idx_pet_day_event_pet_type_date ON pet_day_event (pet_id, type, local_date);

CREATE TEMP TABLE tmp_visit_check AS
SELECT id AS visit_id, gen_random_uuid() AS event_id, pet_id, next_check_on
FROM pet_vet_visit
WHERE next_check_on >= CURRENT_DATE;

INSERT INTO pet_day_event (id, pet_id, type, title, local_date, start_minute, duration_minutes, recurrence, plan_around, created_at)
SELECT event_id, pet_id, 'VET', NULL, next_check_on, 600, 60, 'NONE', TRUE, now()
FROM tmp_visit_check;

UPDATE pet_vet_visit v
SET next_check_event_id = t.event_id
FROM tmp_visit_check t
WHERE v.id = t.visit_id;

DROP TABLE tmp_visit_check;

ALTER TABLE pet_vet_visit DROP CONSTRAINT ck_pet_vet_visit_next_check;
ALTER TABLE pet_vet_visit DROP COLUMN next_check_on;
