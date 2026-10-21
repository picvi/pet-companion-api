package com.picvi.pet_companion_service.triage.service;

import com.picvi.pet_companion_service.common.ClientDateValidator;
import com.picvi.pet_companion_service.pet.service.PetService;
import com.picvi.pet_companion_service.triage.domain.CheckStatus;
import com.picvi.pet_companion_service.triage.domain.TriageLimits;
import com.picvi.pet_companion_service.triage.dto.CheckResponse;
import com.picvi.pet_companion_service.triage.dto.OpenChecksResponse;
import com.picvi.pet_companion_service.triage.entity.PetCheck;
import com.picvi.pet_companion_service.triage.repository.PetCheckRepository;
import com.picvi.pet_companion_service.exception.pet.PetNotFound;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CheckReadService {
    private final PetService petService;
    private final PetCheckRepository checkRepository;
    private final ClientDateValidator clientDateValidator;
    private final Clock clock;

    @Transactional(readOnly = true)
    public CheckResponse get(UUID ownerId, UUID petId, UUID checkId) {
        this.petService.getOwnedPetProfile(ownerId, petId);
        PetCheck check = this.checkRepository.findByIdAndPetId(checkId, petId).orElseThrow(PetNotFound::new);
        return this.toResponse(check);
    }

    @Transactional(readOnly = true)
    public OpenChecksResponse open(UUID ownerId, UUID petId, LocalDate date) {
        this.clientDateValidator.validate(date);
        this.petService.getOwnedPetProfile(ownerId, petId);
        Instant now = Instant.now(this.clock);
        List<OpenChecksResponse.OpenCheck> checks = this.checkRepository
                .findAllByPetIdAndStatusOrderByOccurredOnDescMinuteOfDayDesc(petId, CheckStatus.WATCHING)
                .stream()
                .filter(check -> check.getCheckBackDueAt() != null)
                .map(check -> {
                    Instant dueAt = check.getCheckBackDueAt();
                    String state = dueAt.isAfter(now) ? "UPCOMING"
                            : dueAt.plus(Duration.ofHours(TriageLimits.LAPSE_HOURS)).isAfter(now) ? "DUE" : "LAPSED";
                    return new OpenChecksResponse.OpenCheck(this.toResponse(check), state);
                })
                .toList();
        return new OpenChecksResponse(date, checks);
    }

    private CheckResponse toResponse(PetCheck check) {
        return new CheckResponse(check.getId(), check.getClientId(), check.getPetId(), check.getOccurredOn(), check.getMinuteOfDay(),
                check.getSource(), check.getItem(), check.getCategory(), check.getFlags(), check.getAnswers(),
                check.getSubstance(), check.getNote(), check.getContext(), check.getLevel(), check.getRuleId(),
                check.getRulebook(), reviewStatus(check.getResult()), check.getResult(), check.getStatus(), check.getCheckBackDueAt(),
                check.getResolvedOn(), check.getCreatedAt());
    }

    private String reviewStatus(Map<String, Object> result) {
        Object raw = result.get("review");
        if (raw instanceof Map<?, ?> review) {
            Object status = review.get("status");
            return status == null ? "PENDING" : String.valueOf(status);
        }
        return "PENDING";
    }
}
