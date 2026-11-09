package com.picvi.pet_companion_service.nutrition.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Makes the writes to one pet's foods take turns. Without it two requests can both see "one main food" and both add a
 * second, or both start a food change. The lock lasts until the transaction ends and only blocks the same pet.
 */
@Repository
@RequiredArgsConstructor
public class PetDietLock {
    private final JdbcTemplate jdbc;

    public void acquire(UUID petId) {
        this.jdbc.queryForObject("select count(*) from (select pg_advisory_xact_lock(hashtextextended(?, 0))) l",
                Integer.class, petId.toString());
    }
}
