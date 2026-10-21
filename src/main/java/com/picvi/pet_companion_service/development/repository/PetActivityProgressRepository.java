package com.picvi.pet_companion_service.development.repository;

import com.picvi.pet_companion_service.development.entity.PetActivityProgress;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PetActivityProgressRepository extends JpaRepository<PetActivityProgress, UUID> {

    @EntityGraph(attributePaths = "activity")
    List<PetActivityProgress> findAllByPetId(UUID petId);

    Optional<PetActivityProgress> findByPetIdAndActivityId(UUID petId, Long activityId);
}
