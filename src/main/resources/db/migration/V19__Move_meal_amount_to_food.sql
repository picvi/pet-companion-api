-- The rhythm meal amount (grams per meal) becomes the daily grams of a MAIN food. Only pets that set an amount are
-- moved. The product is the owner's own, with an unknown protein, until they pick the real food.
-- pet_rhythm_settings.meal_amount_grams stays for now and is dropped once the app no longer sends it.
CREATE TEMPORARY TABLE moved_meal_amount ON COMMIT DROP AS
SELECT gen_random_uuid() AS product_id,
       gen_random_uuid() AS food_id,
       p.id AS pet_id,
       p.owner_id AS owner_id,
       s.created_on AS started_on,
       LEAST(s.meal_amount_grams * GREATEST(count(m.meal_minute), 1), 10000) AS daily_grams
FROM pet_rhythm_settings s
         JOIN pets p ON p.id = s.pet_id
         LEFT JOIN pet_meal_time m ON m.settings_id = s.id
WHERE s.meal_amount_grams IS NOT NULL
GROUP BY p.id, p.owner_id, s.created_on, s.meal_amount_grams;

INSERT INTO food_product (id, brand, product_name, form, kind_hint, life_stage, breed_size, complete, protein_status, status,
                          normalised_name, created_by, created_at, updated_at)
SELECT product_id, NULL, 'My puppy''s food', 'DRY', 'MAIN', 'ALL', 'ALL', 'UNKNOWN', 'UNKNOWN', 'OWNER_ADDED',
       f_food_normalise('My puppy''s food'), owner_id, now(), now()
FROM moved_meal_amount;

INSERT INTO pet_food (id, pet_id, client_id, product_id, kind, daily_grams, for_training, origin, started_on, status,
                      created_by, created_at)
SELECT food_id, pet_id, gen_random_uuid(), product_id, 'MAIN', daily_grams, FALSE, 'OWNER', started_on, 'ACTIVE',
       owner_id, now()
FROM moved_meal_amount;
