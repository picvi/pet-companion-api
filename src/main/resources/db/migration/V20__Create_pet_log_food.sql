-- Which food(s) a logged meal contained. A mixed meal during a food change is two rows (e.g. 22 g old, 8 g new);
-- a meal without grams has rows with NULL grams.
CREATE TABLE pet_log_food (
    log_id UUID NOT NULL REFERENCES pet_log(id) ON DELETE CASCADE,
    pet_food_id UUID NOT NULL REFERENCES pet_food(id),
    grams INT,
    PRIMARY KEY (log_id, pet_food_id),
    CONSTRAINT ck_pet_log_food_grams CHECK (grams IS NULL OR grams BETWEEN 1 AND 2000)
);

CREATE INDEX idx_pet_log_food_food ON pet_log_food (pet_food_id);
