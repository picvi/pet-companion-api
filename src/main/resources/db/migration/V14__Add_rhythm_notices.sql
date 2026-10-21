ALTER TABLE pet_rhythm_settings ADD COLUMN created_on DATE;
UPDATE pet_rhythm_settings SET created_on = CURRENT_DATE;
ALTER TABLE pet_rhythm_settings ALTER COLUMN created_on SET NOT NULL;
ALTER TABLE pet_rhythm_settings ADD COLUMN band_note_seen_from_weeks INT;
ALTER TABLE pet_rhythm_settings ADD COLUMN checkin_answered_on DATE;
