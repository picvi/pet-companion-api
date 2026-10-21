CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS unaccent;

-- unaccent() is only STABLE; the wrapper is declared IMMUTABLE so it can be used in a generated column and an index.
CREATE FUNCTION f_unaccent(text) RETURNS text
    LANGUAGE sql IMMUTABLE PARALLEL SAFE STRICT
    RETURN public.unaccent('public.unaccent'::regdictionary, $1);

-- Mirrors FoodNameNormaliser: lower case, no accents, punctuation to spaces, collapsed spaces.
CREATE FUNCTION f_food_normalise(text) RETURNS text
    LANGUAGE sql IMMUTABLE PARALLEL SAFE STRICT
    RETURN btrim(regexp_replace(regexp_replace(lower(f_unaccent($1)), '[^[:alnum:][:space:]]', ' ', 'g'), '\s+', ' ', 'g'));

CREATE TABLE ingredient_source (
    code VARCHAR(30) PRIMARY KEY,
    kind VARCHAR(10) NOT NULL,
    parent_code VARCHAR(30) REFERENCES ingredient_source(code),
    allergen_relevant BOOLEAN NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
    replaced_by VARCHAR(30) REFERENCES ingredient_source(code),
    sort_order INT NOT NULL,
    CONSTRAINT ck_ingredient_source_kind CHECK (kind IN ('PROTEIN', 'CARB', 'OTHER')),
    CONSTRAINT ck_ingredient_source_status CHECK (status IN ('ACTIVE', 'DEPRECATED')),
    CONSTRAINT ck_ingredient_source_parent CHECK (parent_code IS NULL OR parent_code <> code)
);

CREATE TABLE food_product (
    id UUID PRIMARY KEY,
    brand VARCHAR(80),
    product_line VARCHAR(80),
    product_name VARCHAR(120) NOT NULL,
    flavour VARCHAR(80),
    form VARCHAR(12) NOT NULL,
    kind_hint VARCHAR(5) NOT NULL,
    life_stage VARCHAR(6) NOT NULL DEFAULT 'ALL',
    breed_size VARCHAR(12) NOT NULL DEFAULT 'ALL',
    complete VARCHAR(7) NOT NULL DEFAULT 'UNKNOWN',
    protein_status VARCHAR(7) NOT NULL,
    status VARCHAR(11) NOT NULL,
    merged_into_id UUID REFERENCES food_product(id),
    normalised_name VARCHAR(400) NOT NULL,
    search_text TEXT GENERATED ALWAYS AS (
        f_food_normalise(coalesce(brand, '') || ' ' || coalesce(product_line, '') || ' ' || product_name || ' ' || coalesce(flavour, ''))
    ) STORED,
    source_url VARCHAR(300),
    checked_on DATE,
    created_by UUID REFERENCES users(id),
    reviewed_by UUID REFERENCES users(id),
    reviewed_on DATE,
    notified_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT ck_food_product_form CHECK (form IN ('DRY', 'WET', 'RAW', 'HOME_COOKED', 'MIXED', 'AIR_DRIED')),
    CONSTRAINT ck_food_product_kind CHECK (kind_hint IN ('MAIN', 'TREAT', 'CHEW')),
    CONSTRAINT ck_food_product_life_stage CHECK (life_stage IN ('PUPPY', 'ADULT', 'SENIOR', 'ALL')),
    CONSTRAINT ck_food_product_breed_size CHECK (breed_size IN ('SMALL', 'MEDIUM_LARGE', 'ALL')),
    CONSTRAINT ck_food_product_complete CHECK (complete IN ('YES', 'NO', 'UNKNOWN')),
    CONSTRAINT ck_food_product_protein_status CHECK (protein_status IN ('KNOWN', 'UNKNOWN')),
    CONSTRAINT ck_food_product_status CHECK (status IN ('PUBLIC', 'OWNER_ADDED', 'REJECTED', 'MERGED')),
    CONSTRAINT ck_food_product_merged CHECK ((status = 'MERGED') = (merged_into_id IS NOT NULL))
);

CREATE TABLE food_product_ingredient (
    product_id UUID NOT NULL REFERENCES food_product(id) ON DELETE CASCADE,
    source_code VARCHAR(30) NOT NULL REFERENCES ingredient_source(code),
    role VARCHAR(5) NOT NULL,
    hydrolysed BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (product_id, source_code),
    CONSTRAINT ck_food_product_ingredient_role CHECK (role IN ('MAIN', 'OTHER'))
);

CREATE TABLE food_product_change (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES food_product(id),
    changed_by UUID REFERENCES users(id),
    changed_at TIMESTAMPTZ NOT NULL,
    reason VARCHAR(14) NOT NULL,
    before JSONB,
    after JSONB,
    CONSTRAINT ck_food_product_change_reason CHECK (reason IN ('CORRECTION', 'REFORMULATION', 'MERGE'))
);

CREATE INDEX idx_food_product_search ON food_product USING GIN (search_text gin_trgm_ops);
CREATE INDEX idx_food_product_status_creator ON food_product (status, created_by);
CREATE INDEX idx_food_product_owner_added_name ON food_product (normalised_name) WHERE status = 'OWNER_ADDED';
CREATE INDEX idx_food_product_ingredient_code ON food_product_ingredient (source_code);
CREATE INDEX idx_food_product_change_product ON food_product_change (product_id);
