package com.picvi.pet_companion_service.health.controller;

import com.picvi.pet_companion_service.health.dto.HealthSummaryResponse;
import com.picvi.pet_companion_service.health.service.HealthSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/pets/{petId}/health-summary")
public class HealthController {
    private final HealthSummaryService summaryService;

    @GetMapping
    public HealthSummaryResponse get(@AuthenticationPrincipal UUID userId,
                                     @PathVariable UUID petId,
                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return this.summaryService.summarise(userId, petId, date);
    }
}
