-- Minimal slice for allergies; the pet profile epic extends it. certainty matters for analytics:
-- an owner's suspicion is not a vet's diagnosis.
CREATE TABLE pet_condition (
    id UUID PRIMARY KEY,
    pet_id UUID NOT NULL REFERENCES pets(id) ON DELETE CASCADE,
    kind VARCHAR(12) NOT NULL,
    ingredient_code VARCHAR(30) REFERENCES ingredient_source(code),
    certainty VARCHAR(12) NOT NULL,
    text VARCHAR(200),
    since DATE,
    resolved_on DATE,
    version BIGINT NOT NULL DEFAULT 0,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT ck_pet_condition_kind CHECK (kind IN ('ALLERGY', 'INTOLERANCE', 'CHRONIC')),
    CONSTRAINT ck_pet_condition_certainty CHECK (certainty IN ('SUSPECTED', 'VET_CONFIRMED')),
    CONSTRAINT ck_pet_condition_what CHECK (ingredient_code IS NOT NULL OR text IS NOT NULL),
    CONSTRAINT ck_pet_condition_ingredient CHECK (ingredient_code IS NULL OR kind IN ('ALLERGY', 'INTOLERANCE')),
    CONSTRAINT ck_pet_condition_dates CHECK (resolved_on IS NULL OR since IS NULL OR resolved_on >= since)
);

CREATE INDEX idx_pet_condition_pet ON pet_condition (pet_id);

-- The analytics entry point: which ingredient each pet ate, from which food, over which days. to_on is the first day
-- the food was no longer given (NULL = still given). Foods entered by mistake are left out. A product with an unknown
-- protein has no ingredient rows, so it does not show up here (unknown is not "none").
-- When volume needs it this becomes a table maintained from events; callers don't change.
CREATE VIEW v_pet_ingredient_exposure AS
SELECT pf.pet_id,
       pf.id AS pet_food_id,
       pf.kind AS food_kind,
       fp.id AS product_id,
       fp.status AS product_status,
       i.source_code,
       s.parent_code,
       s.kind AS source_kind,
       i.role,
       i.hydrolysed,
       pf.started_on AS from_on,
       pf.ended_on AS to_on
FROM pet_food pf
         JOIN food_product fp ON fp.id = pf.product_id
         JOIN food_product_ingredient i ON i.product_id = fp.id
         JOIN ingredient_source s ON s.code = i.source_code
WHERE pf.status <> 'ENTERED_IN_ERROR';
