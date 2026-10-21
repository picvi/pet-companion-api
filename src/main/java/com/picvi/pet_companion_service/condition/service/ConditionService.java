package com.picvi.pet_companion_service.condition.service;

import com.picvi.pet_companion_service.common.ClientDateValidator;
import com.picvi.pet_companion_service.condition.domain.ConditionCertainty;
import com.picvi.pet_companion_service.condition.domain.ConditionRules;
import com.picvi.pet_companion_service.condition.dto.ConditionDto;
import com.picvi.pet_companion_service.condition.dto.ConditionRequest;
import com.picvi.pet_companion_service.condition.dto.ConditionsResponse;
import com.picvi.pet_companion_service.condition.entity.PetCondition;
import com.picvi.pet_companion_service.condition.repository.PetConditionRepository;
import com.picvi.pet_companion_service.exception.condition.ConditionNotFound;
import com.picvi.pet_companion_service.exception.nutrition.VersionConflict;
import com.picvi.pet_companion_service.pet.service.PetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class ConditionService {
    private final ClientDateValidator clientDateValidator;
    private final PetService petService;
    private final PetConditionRepository repository;
    private final ConditionRules rules;
    private final Clock clock;

    @Transactional(readOnly = true)
    public ConditionsResponse list(UUID ownerId, UUID petId, LocalDate date) {
        this.clientDateValidator.validate(date);
        this.petService.getOwnedPetProfile(ownerId, petId);
        return new ConditionsResponse(this.repository.findAllByPetIdOrderByCreatedAtAsc(petId).stream().map(ConditionDto::of).toList());
    }

    public ConditionDto add(UUID ownerId, UUID petId, ConditionRequest request) {
        this.clientDateValidator.validate(request.date());
        this.petService.getOwnedPetProfile(ownerId, petId);
        String ingredient = request.ingredientCode() == null ? null : request.ingredientCode().name();
        String text = blankToNull(request.text());
        this.rules.validate(request.kind(), ingredient, text, request.since(), request.resolvedOn(), request.date());

        PetCondition condition = new PetCondition(petId, request.kind(), ingredient, certainty(request), text, request.since(),
                ownerId, this.clock.instant());
        if (request.resolvedOn() != null) {
            condition.update(request.kind(), ingredient, certainty(request), text, request.since(), request.resolvedOn());
        }
        return ConditionDto.of(this.repository.saveAndFlush(condition));
    }

    public ConditionDto update(UUID ownerId, UUID petId, UUID conditionId, long expectedVersion, ConditionRequest request) {
        this.clientDateValidator.validate(request.date());
        this.petService.getOwnedPetProfile(ownerId, petId);
        PetCondition condition = this.repository.findByIdAndPetId(conditionId, petId).orElseThrow(ConditionNotFound::new);
        if (condition.getVersion() != expectedVersion) {
            throw new VersionConflict();
        }
        String ingredient = request.ingredientCode() == null ? null : request.ingredientCode().name();
        String text = blankToNull(request.text());
        this.rules.validate(request.kind(), ingredient, text, request.since(), request.resolvedOn(), request.date());
        condition.update(request.kind(), ingredient, certainty(request), text, request.since(), request.resolvedOn());
        return ConditionDto.of(this.repository.saveAndFlush(condition));
    }

    public void delete(UUID ownerId, UUID petId, UUID conditionId) {
        this.petService.getOwnedPetProfile(ownerId, petId);
        this.repository.findByIdAndPetId(conditionId, petId).ifPresent(this.repository::delete);
    }

    private static ConditionCertainty certainty(ConditionRequest request) {
        return request.certainty() == null ? ConditionCertainty.SUSPECTED : request.certainty();
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }
}
