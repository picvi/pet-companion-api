package com.picvi.pet_companion_service.rhythm.api;

import com.picvi.pet_companion_service.pet.entity.Pet;
import com.picvi.pet_companion_service.pet.service.PetService;
import com.picvi.pet_companion_service.rhythm.domain.RhythmRule;
import com.picvi.pet_companion_service.rhythm.domain.RhythmRuleResolver;
import com.picvi.pet_companion_service.rhythm.repository.PetRhythmSettingsRepository;
import com.picvi.pet_companion_service.rhythm.service.RhythmRuleLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/** What other modules may ask rhythm. Rhythm passes meals per day into nutrition, so nutrition asks here only for its own endpoint. */
@Component
@RequiredArgsConstructor
public class RhythmApi {
    static final int FALLBACK_MEALS_PER_DAY = 2;

    private final PetRhythmSettingsRepository settingsRepository;
    private final RhythmRuleLoader ruleLoader;
    private final RhythmRuleResolver ruleResolver;
    private final PetService petService;

    /** The owner's meal times when set, otherwise the standard for the dog's age, otherwise two. */
    @Transactional(readOnly = true)
    public int mealsPerDay(UUID petId, LocalDate date) {
        int saved = this.settingsRepository.findByPetId(petId).map(settings -> settings.getMealMinutes().size()).orElse(0);
        if (saved > 0) {
            return saved;
        }
        Pet pet = this.petService.getReference(petId);
        int ageWeeks = (int) ChronoUnit.WEEKS.between(pet.getBirthDate(), date);
        return this.ruleResolver.resolve(this.ruleLoader.loadAll(), ageWeeks, pet.getBreed().getSizeCategory())
                .map(RhythmRule::meals)
                .map(meals -> meals.standard())
                .orElse(FALLBACK_MEALS_PER_DAY);
    }
}
