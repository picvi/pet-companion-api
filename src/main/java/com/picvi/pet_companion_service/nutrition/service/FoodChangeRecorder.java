package com.picvi.pet_companion_service.nutrition.service;

import com.picvi.pet_companion_service.nutrition.entity.FoodTransition;
import com.picvi.pet_companion_service.nutrition.entity.PetFood;
import com.picvi.pet_companion_service.nutrition.repository.FoodTransitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Writes the all-at-once switch, so it is counted like every other food change. */
@Component
@RequiredArgsConstructor
public class FoodChangeRecorder {
    private final FoodTransitionRepository transitionRepository;
    private final Clock clock;

    /** The old food is no longer given from {@code on}; the new one is the whole meal from {@code on}. */
    public void recordReplacement(UUID petId, UUID clientId, PetFood oldFood, PetFood newFood, LocalDate on) {
        oldFood.endOn(on);
        this.transitionRepository.save(FoodTransition.replacement(petId, clientId, oldFood.getId(), newFood.getId(), on,
                this.clock.instant(), OffsetDateTime.now(this.clock)));
    }
}
