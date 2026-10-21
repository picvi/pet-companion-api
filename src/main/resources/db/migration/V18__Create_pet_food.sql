-- ended_on is the first day the food is no longer given, so a food given Mon-Sun has ended_on = the following Monday.
-- A day is "given" when started_on <= day < ended_on.
CREATE TABLE pet_food (
    id UUID PRIMARY KEY,
    pet_id UUID NOT NULL REFERENCES pets(id) ON DELETE CASCADE,
    client_id UUID NOT NULL,
    product_id UUID NOT NULL REFERENCES food_product(id),
    kind VARCHAR(5) NOT NULL,
    daily_grams INT,
    for_training BOOLEAN NOT NULL DEFAULT FALSE,
    origin VARCHAR(7) NOT NULL DEFAULT 'OWNER',
    started_on DATE NOT NULL,
    ended_on DATE,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_by UUID NOT NULL REFERENCES users(id),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_pet_food_client UNIQUE (pet_id, client_id),
    CONSTRAINT ck_pet_food_kind CHECK (kind IN ('MAIN', 'TREAT', 'CHEW')),
    CONSTRAINT ck_pet_food_grams CHECK (daily_grams IS NULL OR daily_grams BETWEEN 1 AND 10000),
    CONSTRAINT ck_pet_food_origin CHECK (origin IN ('BREEDER', 'SHELTER', 'OWNER')),
    CONSTRAINT ck_pet_food_status CHECK (status IN ('ACTIVE', 'ENDED', 'ENTERED_IN_ERROR')),
    CONSTRAINT ck_pet_food_dates CHECK (ended_on IS NULL OR ended_on >= started_on),
    CONSTRAINT ck_pet_food_training CHECK (NOT for_training OR kind = 'TREAT')
);

CREATE INDEX idx_pet_food_pet_status ON pet_food (pet_id, status);
CREATE INDEX idx_pet_food_product ON pet_food (product_id);

-- One-time prompts and tips, one row per pet and notice instead of a column per notice.
CREATE TABLE pet_diet_notice (
    pet_id UUID NOT NULL REFERENCES pets(id) ON DELETE CASCADE,
    code VARCHAR(30) NOT NULL,
    seen_on DATE NOT NULL,
    seen_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (pet_id, code),
    CONSTRAINT ck_pet_diet_notice_code CHECK (code IN
        ('FOOD_ASK_DISMISSED', 'FOOD_ASK_DISMISSED_AGAIN', 'KEEP_FOOD_TIP', 'PROTEIN_ASK', 'RAW_NOTE', 'COMPLEMENTARY_NOTE'))
);
