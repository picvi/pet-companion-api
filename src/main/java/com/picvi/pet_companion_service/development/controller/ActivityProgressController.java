package com.picvi.pet_companion_service.development.controller;

import com.picvi.pet_companion_service.development.dto.ActivityProgressResponse;
import com.picvi.pet_companion_service.development.dto.RecordOutcomeRequest;
import com.picvi.pet_companion_service.development.dto.SkipActivityRequest;
import com.picvi.pet_companion_service.development.service.ActivityProgressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/pets/{petId}/activities/{code}")
public class ActivityProgressController {
    private final ActivityProgressService activityProgressService;

    @PutMapping("/progress")
    public ActivityProgressResponse recordOutcome(@AuthenticationPrincipal UUID userId,
                                                  @PathVariable UUID petId,
                                                  @PathVariable String code,
                                                  @Valid @RequestBody RecordOutcomeRequest request) {
        return this.activityProgressService.recordOutcome(userId, petId, code, request.outcome(), request.date());
    }

    @DeleteMapping("/progress")
    public ActivityProgressResponse undo(@AuthenticationPrincipal UUID userId,
                                         @PathVariable UUID petId,
                                         @PathVariable String code,
                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return this.activityProgressService.undo(userId, petId, code, date);
    }

    @PutMapping("/skip")
    public ActivityProgressResponse skip(@AuthenticationPrincipal UUID userId,
                                         @PathVariable UUID petId,
                                         @PathVariable String code,
                                         @Valid @RequestBody SkipActivityRequest request) {
        return this.activityProgressService.skip(userId, petId, code, request.reason(), request.date());
    }

    @DeleteMapping("/skip")
    public ActivityProgressResponse restore(@AuthenticationPrincipal UUID userId,
                                            @PathVariable UUID petId,
                                            @PathVariable String code,
                                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return this.activityProgressService.restore(userId, petId, code, date);
    }
}
