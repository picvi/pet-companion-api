-- Codes are never renamed or deleted, only DEPRECATED with replaced_by. Mirrors the IngredientCode enum;
-- ReferenceCodeCheck fails startup when they differ. Runs before R__food_reference_catalogue.sql (Flyway sorts
-- repeatable migrations by description), which needs these rows.
INSERT INTO ingredient_source (code, kind, parent_code, allergen_relevant, status, replaced_by, sort_order)
VALUES
    ('POULTRY', 'PROTEIN', NULL, TRUE, 'ACTIVE', NULL, 10),
    ('CHICKEN', 'PROTEIN', NULL, TRUE, 'ACTIVE', NULL, 11),
    ('TURKEY', 'PROTEIN', NULL, TRUE, 'ACTIVE', NULL, 12),
    ('DUCK', 'PROTEIN', NULL, TRUE, 'ACTIVE', NULL, 13),
    ('GOOSE', 'PROTEIN', NULL, TRUE, 'ACTIVE', NULL, 14),
    ('QUAIL', 'PROTEIN', NULL, TRUE, 'ACTIVE', NULL, 15),
    ('PHEASANT', 'PROTEIN', NULL, TRUE, 'ACTIVE', NULL, 16),
    ('GUINEA_FOWL', 'PROTEIN', NULL, TRUE, 'ACTIVE', NULL, 17),
    ('BEEF', 'PROTEIN', NULL, TRUE, 'ACTIVE', NULL, 20),
    ('LAMB', 'PROTEIN', NULL, TRUE, 'ACTIVE', NULL, 21),
    ('PORK', 'PROTEIN', NULL, TRUE, 'ACTIVE', NULL, 22),
    ('VENISON', 'PROTEIN', NULL, TRUE, 'ACTIVE', NULL, 23),
    ('RABBIT', 'PROTEIN', NULL, TRUE, 'ACTIVE', NULL, 24),
    ('GOAT', 'PROTEIN', NULL, TRUE, 'ACTIVE', NULL, 25),
    ('WILD_BOAR', 'PROTEIN', NULL, TRUE, 'ACTIVE', NULL, 26),
    ('HORSE', 'PROTEIN', NULL, TRUE, 'ACTIVE', NULL, 27),
    ('KANGAROO', 'PROTEIN', NULL, TRUE, 'ACTIVE', NULL, 28),
    ('MEAT_UNSPECIFIED', 'PROTEIN', NULL, TRUE, 'ACTIVE', NULL, 29),
    ('FISH', 'PROTEIN', NULL, TRUE, 'ACTIVE', NULL, 30),
    ('SALMON', 'PROTEIN', NULL, TRUE, 'ACTIVE', NULL, 31),
    ('TROUT', 'PROTEIN', NULL, TRUE, 'ACTIVE', NULL, 32),
    ('COD', 'PROTEIN', NULL, TRUE, 'ACTIVE', NULL, 33),
    ('HERRING', 'PROTEIN', NULL, TRUE, 'ACTIVE', NULL, 34),
    ('SARDINE', 'PROTEIN', NULL, TRUE, 'ACTIVE', NULL, 35),
    ('TUNA', 'PROTEIN', NULL, TRUE, 'ACTIVE', NULL, 36),
    ('SHELLFISH', 'PROTEIN', NULL, TRUE, 'ACTIVE', NULL, 37),
    ('EGG', 'PROTEIN', NULL, TRUE, 'ACTIVE', NULL, 40),
    ('DAIRY', 'PROTEIN', NULL, TRUE, 'ACTIVE', NULL, 41),
    ('INSECT', 'PROTEIN', NULL, TRUE, 'ACTIVE', NULL, 42),
    ('RICE', 'CARB', NULL, FALSE, 'ACTIVE', NULL, 100),
    ('WHEAT', 'CARB', NULL, TRUE, 'ACTIVE', NULL, 101),
    ('MAIZE', 'CARB', NULL, TRUE, 'ACTIVE', NULL, 102),
    ('OAT', 'CARB', NULL, FALSE, 'ACTIVE', NULL, 103),
    ('BARLEY', 'CARB', NULL, FALSE, 'ACTIVE', NULL, 104),
    ('SOY', 'CARB', NULL, TRUE, 'ACTIVE', NULL, 105),
    ('PEA', 'CARB', NULL, FALSE, 'ACTIVE', NULL, 106),
    ('POTATO', 'CARB', NULL, FALSE, 'ACTIVE', NULL, 107),
    ('SWEET_POTATO', 'CARB', NULL, FALSE, 'ACTIVE', NULL, 108)
ON CONFLICT (code) DO UPDATE SET
    kind = EXCLUDED.kind,
    allergen_relevant = EXCLUDED.allergen_relevant,
    status = EXCLUDED.status,
    replaced_by = EXCLUDED.replaced_by,
    sort_order = EXCLUDED.sort_order;

-- Generic parents: a label that says only "poultry" or "fish" names the family, not the animal.
UPDATE ingredient_source SET parent_code = 'POULTRY'
WHERE code IN ('CHICKEN', 'TURKEY', 'DUCK', 'GOOSE', 'QUAIL', 'PHEASANT', 'GUINEA_FOWL');

UPDATE ingredient_source SET parent_code = 'FISH'
WHERE code IN ('SALMON', 'TROUT', 'COD', 'HERRING', 'SARDINE', 'TUNA');

UPDATE ingredient_source SET parent_code = 'MEAT_UNSPECIFIED'
WHERE code IN ('BEEF', 'LAMB', 'PORK', 'VENISON', 'RABBIT', 'GOAT', 'WILD_BOAR', 'HORSE', 'KANGAROO');
