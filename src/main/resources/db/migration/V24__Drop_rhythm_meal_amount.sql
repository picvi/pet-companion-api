-- The amount per meal now lives on the main food (pet_food.daily_grams); V19 moved the old values there.
ALTER TABLE pet_rhythm_settings
    DROP COLUMN meal_amount_grams;
