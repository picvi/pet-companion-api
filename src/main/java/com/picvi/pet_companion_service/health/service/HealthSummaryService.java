package com.picvi.pet_companion_service.health.service;

import com.picvi.pet_companion_service.common.ClientDateValidator;
import com.picvi.pet_companion_service.health.domain.DiaryFilter;
import com.picvi.pet_companion_service.health.domain.HealthLimits;
import com.picvi.pet_companion_service.health.domain.HealthSummary;
import com.picvi.pet_companion_service.health.domain.HealthSummaryRule;
import com.picvi.pet_companion_service.health.dto.HealthSummaryResponse;
import com.picvi.pet_companion_service.nutrition.api.NutritionApi;
import com.picvi.pet_companion_service.pet.dto.PetProfile;
import com.picvi.pet_companion_service.pet.service.PetService;
import com.picvi.pet_companion_service.petlog.api.PetLogApi;
import com.picvi.pet_companion_service.petlog.dto.PetLogDto;
import com.picvi.pet_companion_service.triage.api.TriageApi;
import com.picvi.pet_companion_service.triage.domain.CheckStatus;
import com.picvi.pet_companion_service.triage.domain.Level;
import com.picvi.pet_companion_service.health.domain.BodySign;
import com.picvi.pet_companion_service.health.domain.WatchEvent;
import com.picvi.pet_companion_service.health.domain.WatchKind;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class HealthSummaryService {
    private final ClientDateValidator clientDateValidator;
    private final PetService petService;
    private final PetLogApi petLogApi;
    private final NutritionApi nutritionApi;
    private final HealthSummaryRule summaryRule;
    private final DiaryFilter diaryFilter;
    private final TriageApi triageApi;

    public HealthSummaryResponse summarise(UUID userId, UUID petId, LocalDate date) {
        this.clientDateValidator.validate(date);
        PetProfile pet = this.petService.getOwnedPetProfile(userId, petId);

        LocalDate windowStart = date.minusDays(HealthLimits.WATCH_DAYS - 1L);
        List<WatchEvent> triageEvents = this.triageApi.checksBetween(petId, windowStart, date).stream()
                .filter(check -> (check.level() == Level.WATCH || check.level() == Level.CALL_TODAY || check.level() == Level.GO_NOW)
                        && (check.status() == CheckStatus.WATCHING || check.status() == CheckStatus.ESCALATED))
                .map(check -> new WatchEvent(WatchKind.SYMPTOM, null, signFor(check.item()), check.occurredOn(), check.minuteOfDay()))
                .toList();
        HealthSummary summary = this.summaryRule.summarise(date, pet.createdOn(),
                this.petLogApi.logsBetween(petId, windowStart, date),
                this.nutritionApi.changeNow(petId, date, HealthLimits.WATCH_DAYS), triageEvents);

        return HealthSummaryResponse.of(date, summary,
                this.petLogApi.lastTwoWeightsBefore(petId, date),
                this.petLogApi.lastMedicineBefore(petId, date).orElse(null),
                this.diary(petId, date));
    }

    private static BodySign signFor(String item) {
        return switch (item) {
            case "DIARRHOEA", "LOOSE_STOOL" -> BodySign.POO;
            case "VOMITING" -> BodySign.VOMIT;
            case "NOT_EATING", "ATE_SOMETHING" -> BodySign.EATING;
            case "LOW_ENERGY" -> BodySign.ENERGY;
            case "BREATHING" -> BodySign.BREATHING;
            case "SKIN" -> BodySign.SKIN;
            default -> BodySign.OTHER;
        };
    }

    private List<PetLogDto> diary(UUID petId, LocalDate date) {
        return this.petLogApi.logDtosBetween(petId, date.minusDays(HealthLimits.SIGN_DAYS - 1L), date).stream()
                .filter(this.diaryFilter::shows)
                .toList();
    }
}
