package com.picvi.pet_companion_service.condition.controller;

import com.picvi.pet_companion_service.condition.dto.ConditionDto;
import com.picvi.pet_companion_service.condition.dto.ConditionRequest;
import com.picvi.pet_companion_service.condition.dto.ConditionsResponse;
import com.picvi.pet_companion_service.condition.service.ConditionService;
import com.picvi.pet_companion_service.exception.nutrition.InvalidPetFood;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/pets/{petId}/conditions")
public class ConditionController {
    private final ConditionService conditionService;

    @GetMapping
    public ConditionsResponse list(@AuthenticationPrincipal UUID userId,
                                   @PathVariable UUID petId,
                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return this.conditionService.list(userId, petId, date);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConditionDto add(@AuthenticationPrincipal UUID userId,
                            @PathVariable UUID petId,
                            @Valid @RequestBody ConditionRequest request) {
        return this.conditionService.add(userId, petId, request);
    }

    @PutMapping("/{conditionId}")
    public ConditionDto update(@AuthenticationPrincipal UUID userId,
                               @PathVariable UUID petId,
                               @PathVariable UUID conditionId,
                               @RequestHeader("If-Match") String ifMatch,
                               @Valid @RequestBody ConditionRequest request) {
        return this.conditionService.update(userId, petId, conditionId, parseVersion(ifMatch), request);
    }

    @DeleteMapping("/{conditionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UUID userId,
                       @PathVariable UUID petId,
                       @PathVariable UUID conditionId) {
        this.conditionService.delete(userId, petId, conditionId);
    }

    private static long parseVersion(String ifMatch) {
        try {
            return Long.parseLong(ifMatch.replace("W/", "").replace("\"", "").trim());
        } catch (NumberFormatException e) {
            throw new InvalidPetFood("If-Match must be the condition's version");
        }
    }
}
