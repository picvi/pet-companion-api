package com.picvi.pet_companion_service.nutrition.repository;

import com.picvi.pet_companion_service.nutrition.entity.FoodTransitionExtension;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FoodTransitionExtensionRepository extends JpaRepository<FoodTransitionExtension, Long> {

    List<FoodTransitionExtension> findAllByTransitionId(UUID transitionId);

    List<FoodTransitionExtension> findAllByTransitionIdIn(Collection<UUID> transitionIds);

    Optional<FoodTransitionExtension> findByTransitionIdAndStepOrder(UUID transitionId, int stepOrder);

    Optional<FoodTransitionExtension> findFirstByTransitionIdOrderByCreatedAtDescIdDesc(UUID transitionId);
}
