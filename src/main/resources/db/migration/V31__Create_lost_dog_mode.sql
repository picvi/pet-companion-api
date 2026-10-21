CREATE TABLE pet_lost_profile (
    pet_id UUID PRIMARY KEY REFERENCES pets(id) ON DELETE CASCADE,
    chip_registry VARCHAR(24),
    chip_registry_name VARCHAR(100),
    poster_phone VARCHAR(32),
    markings VARCHAR(300),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_pet_lost_profile_registry CHECK (chip_registry IS NULL OR chip_registry IN ('SAFE_ANIMAL', 'OTHER', 'NOT_REGISTERED', 'NOT_SURE', 'KROPIK')),
    CONSTRAINT ck_pet_lost_profile_other_registry CHECK ((chip_registry = 'OTHER' AND chip_registry_name IS NOT NULL AND length(trim(chip_registry_name)) > 0) OR (chip_registry IS DISTINCT FROM 'OTHER' AND chip_registry_name IS NULL))
);

CREATE TABLE lost_report (
    id UUID PRIMARY KEY,
    pet_id UUID NOT NULL REFERENCES pets(id) ON DELETE CASCADE,
    client_id UUID NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_seen_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_seen_place VARCHAR(300) NOT NULL,
    last_seen_lat DOUBLE PRECISION,
    last_seen_lng DOUBLE PRECISION,
    poster_phone VARCHAR(32),
    status VARCHAR(16) NOT NULL,
    ended_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_lost_report_pet_client UNIQUE (pet_id, client_id),
    CONSTRAINT ck_lost_report_status CHECK (status IN ('ACTIVE', 'FOUND', 'STOPPED')),
    CONSTRAINT ck_lost_report_end_state CHECK ((status = 'ACTIVE' AND ended_at IS NULL) OR (status <> 'ACTIVE' AND ended_at IS NOT NULL)),
    CONSTRAINT ck_lost_report_last_seen CHECK (last_seen_at <= started_at),
    CONSTRAINT ck_lost_report_coordinates_pair CHECK ((last_seen_lat IS NULL AND last_seen_lng IS NULL) OR (last_seen_lat IS NOT NULL AND last_seen_lng IS NOT NULL)),
    CONSTRAINT ck_lost_report_lat CHECK (last_seen_lat IS NULL OR last_seen_lat BETWEEN -90 AND 90),
    CONSTRAINT ck_lost_report_lng CHECK (last_seen_lng IS NULL OR last_seen_lng BETWEEN -180 AND 180)
);

CREATE UNIQUE INDEX uq_lost_report_active_pet ON lost_report (pet_id) WHERE status = 'ACTIVE';
CREATE INDEX idx_lost_report_pet_started ON lost_report (pet_id, started_at DESC);

CREATE TABLE lost_report_step (
    report_id UUID NOT NULL REFERENCES lost_report(id) ON DELETE CASCADE,
    step_code VARCHAR(32) NOT NULL,
    done_at TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (report_id, step_code),
    CONSTRAINT ck_lost_report_step_code CHECK (step_code IN ('CHIP_REGISTRY', 'SHELTER', 'MUNICIPAL_GUARD', 'NEARBY_VETS', 'SHARE_POSTER', 'SHARE_WITH_FAMILY'))
);
