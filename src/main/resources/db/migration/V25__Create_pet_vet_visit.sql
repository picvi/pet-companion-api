CREATE TABLE pet_vet_visit (
    id UUID PRIMARY KEY,
    pet_id UUID NOT NULL REFERENCES pets(id) ON DELETE CASCADE,
    client_id UUID NOT NULL,
    visited_on DATE NOT NULL,
    reason VARCHAR(24) NOT NULL,
    outcomes VARCHAR(64) NOT NULL DEFAULT '',
    clinic VARCHAR(120),
    note VARCHAR(500),
    next_check_on DATE,
    brief_helpful VARCHAR(8),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_pet_vet_visit_client UNIQUE (pet_id, client_id),
    CONSTRAINT ck_pet_vet_visit_reason CHECK (reason IN ('SOMETHING_WRONG', 'CHECK_UP', 'VACCINE_OR_TREATMENT')),
    CONSTRAINT ck_pet_vet_visit_brief_helpful CHECK (brief_helpful IS NULL OR brief_helpful IN ('YES', 'A_BIT', 'NO')),
    CONSTRAINT ck_pet_vet_visit_next_check CHECK (next_check_on IS NULL OR next_check_on > visited_on)
);

CREATE INDEX idx_pet_vet_visit_pet_date ON pet_vet_visit (pet_id, visited_on DESC);
