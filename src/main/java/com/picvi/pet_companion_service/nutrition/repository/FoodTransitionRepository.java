package com.picvi.pet_companion_service.nutrition.repository;

import com.picvi.pet_companion_service.nutrition.domain.TransitionStatus;
import com.picvi.pet_companion_service.nutrition.entity.FoodTransition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FoodTransitionRepository extends JpaRepository<FoodTransition, UUID> {

    Optional<FoodTransition> findByIdAndPetId(UUID id, UUID petId);

    Optional<FoodTransition> findByPetIdAndClientId(UUID petId, UUID clientId);

    @Query("select t from FoodTransition t where t.petId = :petId and t.status in :open")
    Optional<FoodTransition> findOpen(@Param("petId") UUID petId, @Param("open") List<TransitionStatus> open);

    /** A finished change the owner has not acknowledged yet (the "all done" banner). */
    @Query("select t from FoodTransition t where t.petId = :petId and t.status = :done and t.finishedSeenAt is null")
    Optional<FoodTransition> findFinishedUnseen(@Param("petId") UUID petId, @Param("done") TransitionStatus done);

    List<FoodTransition> findAllByPetIdOrderByStartOnDesc(UUID petId);
}
