CREATE TABLE pet_tried_food (
    pet_id UUID NOT NULL REFERENCES pets (id) ON DELETE CASCADE,
    food_code VARCHAR(40) NOT NULL REFERENCES food_safety_item (code),
    tried_on DATE NOT NULL,
    reaction VARCHAR(12) NOT NULL,
    note VARCHAR(200),
    created_by UUID REFERENCES users (id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (pet_id, food_code),
    CONSTRAINT ck_pet_tried_food_reaction CHECK (reaction IN ('LOVED_IT', 'FINE', 'NOT_INTO_IT', 'UPSET_TUMMY'))
);
