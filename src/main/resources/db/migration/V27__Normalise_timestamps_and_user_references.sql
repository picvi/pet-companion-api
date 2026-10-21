-- Columns written from the application Clock (UTC) are converted as UTC; columns filled by JPA auditing were written in the
-- JVM zone of the dev machine (Europe/Warsaw). Only dev data existed when this ran.
ALTER TABLE breeds
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'Europe/Warsaw',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'Europe/Warsaw';

ALTER TABLE care_event
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'Europe/Warsaw';

ALTER TABLE development_activity
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'Europe/Warsaw',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'Europe/Warsaw';

ALTER TABLE pet_activity_log
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'Europe/Warsaw';

ALTER TABLE pet_activity_progress
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'Europe/Warsaw',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'Europe/Warsaw';

ALTER TABLE pet_day_event
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'Europe/Warsaw';

ALTER TABLE pets
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'Europe/Warsaw',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'Europe/Warsaw';

ALTER TABLE users
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'Europe/Warsaw',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'Europe/Warsaw';

ALTER TABLE pet_log
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'Europe/Warsaw',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

ALTER TABLE food_product
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

ALTER TABLE food_transition
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';

ALTER TABLE pet_condition
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';

ALTER TABLE pet_food
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';

ALTER TABLE pet_vet_visit
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

ALTER TABLE pet_log
    ALTER COLUMN logged_by DROP NOT NULL,
    DROP CONSTRAINT pet_log_logged_by_fkey,
    ADD CONSTRAINT pet_log_logged_by_fkey FOREIGN KEY (logged_by) REFERENCES users (id) ON DELETE SET NULL;

ALTER TABLE pet_condition
    ALTER COLUMN created_by DROP NOT NULL,
    DROP CONSTRAINT pet_condition_created_by_fkey,
    ADD CONSTRAINT pet_condition_created_by_fkey FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL;

ALTER TABLE pet_food
    ALTER COLUMN created_by DROP NOT NULL,
    DROP CONSTRAINT pet_food_created_by_fkey,
    ADD CONSTRAINT pet_food_created_by_fkey FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL;

ALTER TABLE food_product
    DROP CONSTRAINT food_product_created_by_fkey,
    ADD CONSTRAINT food_product_created_by_fkey FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL,
    DROP CONSTRAINT food_product_reviewed_by_fkey,
    ADD CONSTRAINT food_product_reviewed_by_fkey FOREIGN KEY (reviewed_by) REFERENCES users (id) ON DELETE SET NULL;

ALTER TABLE food_product_change
    DROP CONSTRAINT food_product_change_changed_by_fkey,
    ADD CONSTRAINT food_product_change_changed_by_fkey FOREIGN KEY (changed_by) REFERENCES users (id) ON DELETE SET NULL;
