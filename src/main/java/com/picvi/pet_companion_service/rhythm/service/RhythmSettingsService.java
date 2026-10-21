package com.picvi.pet_companion_service.rhythm.service;

import com.picvi.pet_companion_service.common.ClientDateValidator;
import com.picvi.pet_companion_service.exception.InvalidDate;
import com.picvi.pet_companion_service.pet.dto.PetProfile;
import com.picvi.pet_companion_service.pet.service.PetService;
import com.picvi.pet_companion_service.rhythm.domain.DaySettings;
import com.picvi.pet_companion_service.rhythm.domain.RhythmRule;
import com.picvi.pet_companion_service.rhythm.domain.RhythmRuleResolver;
import com.picvi.pet_companion_service.rhythm.domain.RhythmSettingsValidator;
import com.picvi.pet_companion_service.rhythm.dto.RhythmDayResponse;
import com.picvi.pet_companion_service.rhythm.entity.PetRhythmSettings;
import com.picvi.pet_companion_service.rhythm.repository.PetRhythmSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class RhythmSettingsService {
    private final ClientDateValidator clientDateValidator;
    private final PetService petService;
    private final RhythmRuleLoader ruleLoader;
    private final RhythmRuleResolver ruleResolver;
    private final RhythmSettingsValidator settingsValidator;
    private final PetRhythmSettingsRepository settingsRepository;
    private final RhythmService rhythmService;

    public RhythmDayResponse update(UUID ownerId, UUID petId, DaySettings requested, LocalDate date) {
        this.clientDateValidator.validate(date);
        PetProfile profile = this.petService.getOwnedPetProfile(ownerId, petId);
        DaySettings settings = this.sorted(requested);
        RhythmRule rule = this.ruleFor(profile, date);
        this.settingsValidator.validate(rule, settings);

        PetRhythmSettings entity = this.settingsRepository.findByPetId(petId)
                .orElseGet(() -> new PetRhythmSettings(this.petService.getReference(petId), date, rule.fromWeeks()));
        entity.setWakeMinute(settings.wakeMinute());
        entity.setBedMinute(settings.bedMinute());
        entity.replaceMealMinutes(settings.mealMinutes());
        entity.setCustomAwakeMinutes(settings.customAwakeMinutes());
        this.settingsRepository.save(entity);

        return this.rhythmService.buildDay(profile, date);
    }

    public RhythmDayResponse markBandNoteSeen(UUID ownerId, UUID petId, LocalDate date) {
        this.clientDateValidator.validate(date);
        PetProfile profile = this.petService.getOwnedPetProfile(ownerId, petId);
        RhythmRule rule = this.ruleFor(profile, date);

        this.settingsRepository.findByPetId(petId).ifPresent(settings -> {
            settings.setBandNoteSeenFromWeeks(rule.fromWeeks());
            this.settingsRepository.save(settings);
        });
        return this.rhythmService.buildDay(profile, date);
    }

    public RhythmDayResponse answerCheckin(UUID ownerId, UUID petId, LocalDate date) {
        this.clientDateValidator.validate(date);
        PetProfile profile = this.petService.getOwnedPetProfile(ownerId, petId);

        this.settingsRepository.findByPetId(petId).ifPresent(settings -> {
            settings.setCheckinAnsweredOn(date);
            this.settingsRepository.save(settings);
        });
        return this.rhythmService.buildDay(profile, date);
    }

    private RhythmRule ruleFor(PetProfile profile, LocalDate date) {
        if (date.isBefore(profile.birthDate())) {
            throw new InvalidDate();
        }
        int ageWeeks = (int) ChronoUnit.WEEKS.between(profile.birthDate(), date);
        return this.ruleResolver.resolve(this.ruleLoader.loadAll(), ageWeeks, profile.sizeCategory())
                .orElseThrow(InvalidDate::new);
    }

    private DaySettings sorted(DaySettings settings) {
        List<Integer> meals = settings.mealMinutes().stream().sorted().toList();
        return new DaySettings(settings.wakeMinute(), settings.bedMinute(), meals,
                settings.customAwakeMinutes());
    }
}
