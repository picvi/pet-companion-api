package com.picvi.pet_companion_service.development.controller;

import com.picvi.pet_companion_service.development.dto.AloneOverviewResponse;
import com.picvi.pet_companion_service.development.dto.AloneSessionRequest;
import com.picvi.pet_companion_service.development.dto.AloneSessionResponse;
import com.picvi.pet_companion_service.development.service.AloneSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/pets/{petId}/activities/{code}/sessions")
public class AloneSessionController {
    private final AloneSessionService aloneSessionService;

    @PostMapping
    public AloneSessionResponse record(@AuthenticationPrincipal UUID userId,
                                       @PathVariable UUID petId,
                                       @PathVariable String code,
                                       @Valid @RequestBody AloneSessionRequest request) {
        return this.aloneSessionService.record(userId, petId, code, request.date(),
                request.durationSeconds(), request.reaction());
    }

    @GetMapping
    public AloneOverviewResponse overview(@AuthenticationPrincipal UUID userId,
                                          @PathVariable UUID petId,
                                          @PathVariable String code,
                                          @RequestParam(defaultValue = "7") int limit) {
        return this.aloneSessionService.overview(userId, petId, code, Math.clamp(limit, 1, 50));
    }

    @DeleteMapping("/{sessionId}")
    public AloneOverviewResponse undo(@AuthenticationPrincipal UUID userId,
                                      @PathVariable UUID petId,
                                      @PathVariable String code,
                                      @PathVariable UUID sessionId) {
        return this.aloneSessionService.undo(userId, petId, code, sessionId);
    }
}
