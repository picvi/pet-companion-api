package com.picvi.pet_companion_service.nutrition.repository;

import com.picvi.pet_companion_service.nutrition.entity.FoodTransitionPause;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FoodTransitionPauseRepository extends JpaRepository<FoodTransitionPause, Long> {

    List<FoodTransitionPause> findAllByTransitionIdOrderByPausedOnAsc(UUID transitionId);

    List<FoodTransitionPause> findAllByTransitionIdIn(Collection<UUID> transitionIds);

    Optional<FoodTransitionPause> findFirstByTransitionIdAndResumedOnIsNull(UUID transitionId);
}
