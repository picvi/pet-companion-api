package com.picvi.pet_companion_service.triage.service;

import com.picvi.pet_companion_service.care.api.CareApi;
import com.picvi.pet_companion_service.care.api.CoreVaccineProgress;
import com.picvi.pet_companion_service.common.ClientDateValidator;
import com.picvi.pet_companion_service.exception.triage.InvalidCheck;
import com.picvi.pet_companion_service.nutrition.api.NutritionApi;
import com.picvi.pet_companion_service.pet.dto.PetProfile;
import com.picvi.pet_companion_service.pet.dto.PetResponse;
import com.picvi.pet_companion_service.pet.service.PetService;
import com.picvi.pet_companion_service.petlog.api.LogPredicates;
import com.picvi.pet_companion_service.petlog.api.PetLogApi;
import com.picvi.pet_companion_service.triage.domain.Context;
import com.picvi.pet_companion_service.triage.domain.VaccineHistory;
import com.picvi.pet_companion_service.triage.dto.CheckContextResponse;
import com.picvi.pet_companion_service.triage.repository.PetCheckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CheckContextService {
    private final PetService petService;
    private final PetLogApi petLogApi;
    private final CareApi careApi;
    private final NutritionApi nutritionApi;
    private final PetCheckRepository checkRepository;
    private final CatalogLoader catalogLoader;
    private final ClientDateValidator clientDateValidator;
    private final Clock clock;

    @Transactional(readOnly = true)
    public CheckContextResponse get(UUID ownerId, UUID petId, String item, LocalDate date) {
        this.clientDateValidator.validate(date);
        if (this.catalogLoader.getCatalog().item(item) == null) {
            throw new InvalidCheck("item", "INVALID");
        }
        PetProfile profile = this.petService.getOwnedPetProfile(ownerId, petId);
        PetResponse pet = this.petService.getPetById(ownerId, petId);
        int ageWeeks = Math.max(0, Math.toIntExact(ChronoUnit.DAYS.between(profile.birthDate(), date) / 7));
        CoreVaccineProgress vaccines = this.careApi.coreVaccineDoses(ownerId, petId, date);
        List<PetLogApi.LogSnapshot> relatedLogs = this.petLogApi.logsBetween(petId, date.minusDays(6), date).stream()
                .filter(log -> this.isRelated(item, log))
                .toList();
        LocalDateTime checkMoment = LocalDateTime.of(date, LocalTime.now(this.clock));
        LocalDateTime since24h = checkMoment.minusHours(24);
        long related24h = relatedLogs.stream().filter(log -> {
            LocalDateTime loggedAt = LocalDateTime.of(log.localDate(), LocalTime.of(log.minuteOfDay() / 60, log.minuteOfDay() % 60));
            return !loggedAt.isBefore(since24h) && !loggedAt.isAfter(checkMoment);
        }).count();
        Integer related7d = relatedLogs.size();
        LocalDate firstRelatedOn = relatedLogs.stream().map(PetLogApi.LogSnapshot::localDate).min(LocalDate::compareTo).orElse(null);
        Integer relatedChecks24h = Math.toIntExact(this.checkRepository
                .findAllByPetIdAndOccurredOnBetweenOrderByOccurredOnAscMinuteOfDayAsc(petId, date.minusDays(1), date).stream()
                .filter(check -> check.getItem().equals(item))
                .filter(check -> {
                    LocalDateTime checkedAt = LocalDateTime.of(check.getOccurredOn(), LocalTime.of(check.getMinuteOfDay() / 60, check.getMinuteOfDay() % 60));
                    return !checkedAt.isBefore(since24h) && !checkedAt.isAfter(checkMoment);
                }).count());
        OptionalInt foodChangeDays = this.nutritionApi.daysSinceFoodChange(petId, date);
        Integer daysSinceFoodChange = foodChangeDays.isPresent() ? foodChangeDays.getAsInt() : null;
        Integer count24h = Math.toIntExact(related24h);
        Context facts = new Context(ageWeeks, profile.sizeCategory().name(), pet.breedCode(), vaccines.done(),
                vaccines.total(), vaccines.done() == 0 ? VaccineHistory.UNKNOWN : VaccineHistory.KNOWN,
                daysSinceFoodChange, null, count24h, related7d, relatedChecks24h, null);
        Map<String, String> prefill = related24h == 0 ? Map.of()
                : Map.of("TIMES_24H", count24h == 1 ? "ONE" : count24h <= 3 ? "FEW" : "MANY");
        return new CheckContextResponse(item, ageWeeks, profile.sizeCategory(), pet.breedCode(), vaccines.done(),
                vaccines.total(), vaccines.done() == 0 ? VaccineHistory.UNKNOWN : VaccineHistory.KNOWN,
                daysSinceFoodChange, null, new CheckContextResponse.Related(count24h, related7d, firstRelatedOn), prefill);
    }

    private boolean isRelated(String item, PetLogApi.LogSnapshot log) {
        return switch (item) {
            case "DIARRHOEA" -> LogPredicates.looseStool(log);
            case "VOMITING" -> LogPredicates.vomiting(log);
            case "NOT_EATING" -> LogPredicates.leftFood(log);
            case "LOW_ENERGY" -> LogPredicates.lowEnergy(log);
            default -> false;
        };
    }
}
