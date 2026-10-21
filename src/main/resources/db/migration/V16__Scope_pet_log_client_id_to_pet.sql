ALTER TABLE pet_log
    DROP CONSTRAINT pet_log_client_id_key,
    ADD CONSTRAINT uq_pet_log_pet_client UNIQUE (pet_id, client_id);
