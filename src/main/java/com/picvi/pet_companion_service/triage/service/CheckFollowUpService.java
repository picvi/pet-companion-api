package com.picvi.pet_companion_service.triage.service;

import com.picvi.pet_companion_service.common.ClientDateValidator;
import com.picvi.pet_companion_service.exception.pet.PetNotFound;
import com.picvi.pet_companion_service.exception.triage.CheckConflict;
import com.picvi.pet_companion_service.pet.service.PetService;
import com.picvi.pet_companion_service.triage.domain.CheckStatus;
import com.picvi.pet_companion_service.triage.domain.FollowUpAnswer;
import com.picvi.pet_companion_service.triage.domain.FollowUpOutcome;
import com.picvi.pet_companion_service.triage.domain.FollowUpPolicy;
import com.picvi.pet_companion_service.triage.domain.Level;
import com.picvi.pet_companion_service.triage.domain.TriageLimits;
import com.picvi.pet_companion_service.triage.dto.CheckResponse;
import com.picvi.pet_companion_service.triage.dto.FollowUpRequest;
import com.picvi.pet_companion_service.triage.entity.PetCheck;
import com.picvi.pet_companion_service.triage.entity.PetCheckFollowUp;
import com.picvi.pet_companion_service.triage.repository.PetCheckFollowUpRepository;
import com.picvi.pet_companion_service.triage.repository.PetCheckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CheckFollowUpService {
    private final PetService petService;
    private final PetCheckRepository checkRepository;
    private final PetCheckFollowUpRepository followUpRepository;
    private final CheckReadService readService;
    private final FollowUpPolicy followUpPolicy;
    private final ClientDateValidator clientDateValidator;
    private final Clock clock;

    @Transactional
    public CheckResponse answer(UUID ownerId, UUID petId, UUID checkId, FollowUpRequest request) {
        this.petService.getOwnedPetProfile(ownerId, petId);
        PetCheck check = this.checkRepository.findByIdAndPetId(checkId, petId).orElseThrow(PetNotFound::new);
        var previous = this.followUpRepository.findByCheckIdAndClientId(checkId, request.clientId());
        if (previous.isPresent()) return this.readService.get(ownerId, petId, checkId);
        if (check.getStatus() != CheckStatus.WATCHING) throw new CheckConflict("CHECK_NOT_WATCHING");
        LocalDate today = LocalDate.now(this.clock);
        this.clientDateValidator.validate(today);
        Instant now = Instant.now(this.clock);
        int sameInARow = this.sameInARow(checkId);
        int originalInterval = this.originalCheckBackMinutes(check.getResult());
        FollowUpOutcome outcome = this.followUpPolicy.apply(check.getLevel(), request.answer(), sameInARow, originalInterval);
        Instant dueAt = outcome.checkBackMinutes() == null ? null : now.plus(Duration.ofMinutes(outcome.checkBackMinutes()));
        Map<String, Object> result = this.updatedResult(check, request.answer(), outcome, dueAt);
        java.time.LocalTime localTime = java.time.LocalTime.now(this.clock);
        int minuteOfDay = localTime.getHour() * 60 + localTime.getMinute();
        PetCheckFollowUp followUp = new PetCheckFollowUp(checkId, request.clientId(), request.answer(), outcome.level(),
                today, minuteOfDay, now);
        this.followUpRepository.save(followUp);
        check.updateFollowUp(outcome.level(), outcome.status(), result, dueAt,
                outcome.status() == CheckStatus.RESOLVED ? today : null, now);
        return this.readService.get(ownerId, petId, checkId);
    }

    @Transactional
    public CheckResponse dismiss(UUID ownerId, UUID petId, UUID checkId) {
        this.petService.getOwnedPetProfile(ownerId, petId);
        PetCheck check = this.checkRepository.findByIdAndPetId(checkId, petId).orElseThrow(PetNotFound::new);
        if (check.getStatus() == CheckStatus.DISMISSED) return this.readService.get(ownerId, petId, checkId);
        if (check.getStatus() != CheckStatus.WATCHING || check.getCheckBackDueAt() == null
                || check.getCheckBackDueAt().plus(Duration.ofHours(TriageLimits.LAPSE_HOURS)).isAfter(Instant.now(this.clock))) {
            throw new CheckConflict("CHECK_NOT_LAPSED");
        }
        Instant now = Instant.now(this.clock);
        Map<String, Object> result = new LinkedHashMap<>(check.getResult());
        result.put("status", CheckStatus.DISMISSED.name());
        check.updateFollowUp(check.getLevel(), CheckStatus.DISMISSED, result, null, null, now);
        return this.readService.get(ownerId, petId, checkId);
    }

    private int sameInARow(UUID checkId) {
        List<PetCheckFollowUp> history = this.followUpRepository.findAllByCheckIdOrderByAnsweredOnAscMinuteOfDayAsc(checkId);
        int count = 0;
        for (int index = history.size() - 1; index >= 0; index--) {
            if (history.get(index).getAnswer() != FollowUpAnswer.SAME) break;
            count++;
        }
        return count;
    }

    private int originalCheckBackMinutes(Map<String, Object> result) {
        Object original = result.get("original");
        Map<?, ?> originalResult = original instanceof Map<?, ?> map ? map : result;
        Object checkBack = originalResult.get("checkBack");
        if (checkBack instanceof Map<?, ?> map && map.get("afterMinutes") instanceof Number number) return number.intValue();
        return TriageLimits.MIN_REPEAT_MINUTES * 2;
    }

    private Map<String, Object> updatedResult(PetCheck check, FollowUpAnswer answer, FollowUpOutcome outcome, Instant dueAt) {
        Map<String, Object> result = new LinkedHashMap<>();
        Object original = check.getResult().getOrDefault("original", check.getResult());
        result.put("original", original);
        result.put("reason", answer == FollowUpAnswer.BETTER ? check.getResult().get("reason")
                : Map.of("code", outcome.reason().name(), "params", Map.of()));
        result.put("steps", answer == FollowUpAnswer.BETTER ? check.getResult().get("steps") : this.escalationSteps(outcome.level()));
        result.put("changeIf", check.getResult().getOrDefault("changeIf", Map.of("to", Level.GO_NOW, "codes", List.of())));
        result.put("actions", answer == FollowUpAnswer.BETTER ? check.getResult().get("actions") : this.escalationActions(outcome.level(), check.getItem()));
        result.put("checkBack", dueAt == null ? null : Map.of("afterMinutes", outcome.checkBackMinutes(), "dueAt", dueAt.toString()));
        result.put("status", outcome.status().name());
        result.put("review", check.getResult().getOrDefault("review", Map.of("status", "PENDING", "testOnly", true)));
        result.put("caseIds", check.getResult().getOrDefault("caseIds", List.of()));
        return result;
    }

    private List<Map<String, Object>> escalationSteps(Level level) {
        String code = level == Level.GO_NOW ? "SEEK_URGENT_VETERINARY_CARE" : "CONTACT_VETERINARIAN_TODAY";
        return List.of(Map.of("code", code, "params", Map.of()));
    }

    private List<Map<String, Object>> escalationActions(Level level, String concern) {
        if (level.ordinal() < Level.CALL_TODAY.ordinal()) return List.of();
        return List.of(Map.of("code", "PREPARE_VET_BRIEF", "params", Map.of("concern", concern)));
    }
}
