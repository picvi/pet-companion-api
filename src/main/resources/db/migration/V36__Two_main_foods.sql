ALTER TABLE pet_food DROP CONSTRAINT ck_pet_food_origin;
ALTER TABLE pet_food DROP COLUMN origin;

ALTER TABLE pet_log_food DROP CONSTRAINT ck_pet_log_food_grams;
ALTER TABLE pet_log_food DROP COLUMN grams;

CREATE UNIQUE INDEX uq_pet_food_active_main_product ON pet_food (pet_id, product_id)
    WHERE kind = 'MAIN' AND status = 'ACTIVE';
