package com.picvi.pet_companion_service.pet.repository;

import com.picvi.pet_companion_service.pet.entity.Pet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PetRepository extends JpaRepository<Pet, UUID> {

    boolean existsByOwnerId(UUID ownerId);

    Optional<Pet> findFirstByOwnerId(UUID ownerId);

    Optional<Pet> findByIdAndOwnerId(UUID id, UUID ownerId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM pets WHERE owner_id = :ownerId", nativeQuery = true)
    int deleteAllByOwnerId(@Param("ownerId") UUID ownerId);
}
