ALTER TABLE pet_vet_visit
    ADD COLUMN status VARCHAR(12) NOT NULL DEFAULT 'DONE',
    ADD COLUMN scheduled_minute INT,
    ADD COLUMN prep_note VARCHAR(300),
    ADD COLUMN follow_up_of UUID REFERENCES pet_vet_visit (id) ON DELETE SET NULL,
    ADD COLUMN just_check BOOLEAN NOT NULL DEFAULT FALSE,
    ADD CONSTRAINT ck_pet_vet_visit_status CHECK (status IN ('PLANNED', 'DONE')),
    ADD CONSTRAINT ck_pet_vet_visit_scheduled_minute CHECK (scheduled_minute IS NULL OR scheduled_minute BETWEEN 0 AND 1439),
    ADD CONSTRAINT ck_pet_vet_visit_planned CHECK (status <> 'PLANNED' OR scheduled_minute IS NOT NULL);

ALTER TABLE pet_vet_visit DROP CONSTRAINT ck_pet_vet_visit_reason;
ALTER TABLE pet_vet_visit DROP COLUMN reason;

ALTER TABLE pet_vet_visit RENAME COLUMN next_check_event_id TO day_event_id;
ALTER INDEX uq_pet_vet_visit_next_check_event RENAME TO uq_pet_vet_visit_day_event;

CREATE TEMP TABLE tmp_follow_up AS
SELECT v.id AS done_id, gen_random_uuid() AS plan_id, gen_random_uuid() AS client_id, v.pet_id, v.clinic,
       e.id AS event_id, e.local_date, e.start_minute
FROM pet_vet_visit v
JOIN pet_day_event e ON e.id = v.day_event_id;

UPDATE pet_vet_visit SET day_event_id = NULL WHERE id IN (SELECT done_id FROM tmp_follow_up);

INSERT INTO pet_vet_visit (id, pet_id, client_id, visited_on, outcomes, clinic, status, scheduled_minute,
                           follow_up_of, day_event_id, version, created_at, updated_at)
SELECT plan_id, pet_id, client_id, local_date, '', clinic, 'PLANNED', start_minute,
       done_id, event_id, 0, now(), now()
FROM tmp_follow_up;

DROP TABLE tmp_follow_up;

INSERT INTO pet_vet_visit (id, pet_id, client_id, visited_on, outcomes, status, scheduled_minute,
                           day_event_id, version, created_at, updated_at)
SELECT gen_random_uuid(), e.pet_id, gen_random_uuid(), e.local_date, '', 'PLANNED', e.start_minute,
       e.id, 0, now(), now()
FROM pet_day_event e
WHERE e.type = 'VET' AND e.recurrence = 'NONE'
  AND NOT EXISTS (SELECT 1 FROM pet_vet_visit v WHERE v.day_event_id = e.id);

CREATE INDEX idx_pet_vet_visit_pet_status_date ON pet_vet_visit (pet_id, status, visited_on);

CREATE TABLE pet_vet_visit_care_event (
    visit_id      UUID NOT NULL REFERENCES pet_vet_visit (id) ON DELETE CASCADE,
    care_event_id UUID NOT NULL REFERENCES care_event (id) ON DELETE CASCADE,
    PRIMARY KEY (visit_id, care_event_id)
);

CREATE TABLE pet_vet_visit_log (
    visit_id UUID        NOT NULL REFERENCES pet_vet_visit (id) ON DELETE CASCADE,
    log_id   UUID        NOT NULL REFERENCES pet_log (id) ON DELETE CASCADE,
    role     VARCHAR(16) NOT NULL,
    PRIMARY KEY (visit_id, log_id),
    CONSTRAINT ck_pet_vet_visit_log_role CHECK (role IN ('CLINIC_MEDICINE', 'WEIGHT'))
);

CREATE TABLE pet_vet_visit_course (
    visit_id  UUID NOT NULL REFERENCES pet_vet_visit (id) ON DELETE CASCADE,
    course_id UUID NOT NULL REFERENCES medication_course (id) ON DELETE CASCADE,
    PRIMARY KEY (visit_id, course_id)
);

CREATE TABLE pet_vet_visit_test (
    visit_id            UUID        NOT NULL REFERENCES pet_vet_visit (id) ON DELETE CASCADE,
    kind                VARCHAR(16) NOT NULL,
    waiting_for_results BOOLEAN     NOT NULL DEFAULT FALSE,
    PRIMARY KEY (visit_id, kind),
    CONSTRAINT ck_pet_vet_visit_test_kind CHECK (kind IN ('BLOOD', 'STOOL', 'URINE', 'IMAGING', 'OTHER'))
);

CREATE TABLE pet_vet_visit_procedure (
    visit_id UUID        PRIMARY KEY REFERENCES pet_vet_visit (id) ON DELETE CASCADE,
    kind     VARCHAR(16) NOT NULL,
    note     VARCHAR(120),
    CONSTRAINT ck_pet_vet_visit_procedure_kind CHECK (kind IN ('DENTAL', 'STITCHES', 'MICROCHIP', 'OTHER')),
    CONSTRAINT ck_pet_vet_visit_procedure_note CHECK (kind <> 'OTHER' OR note IS NOT NULL)
);
