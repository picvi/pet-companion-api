ALTER TABLE pets
    ADD COLUMN vaccination_status VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN',
    ADD COLUMN vaccination_status_changed_on DATE,
    ADD COLUMN vaccination_prompt_count INT NOT NULL DEFAULT 0;

ALTER TABLE pets
    ADD CONSTRAINT ck_pets_vaccination_status
        CHECK (vaccination_status IN ('UNKNOWN', 'NONE', 'FIRST_DOSE', 'SERIES_COMPLETE'));
