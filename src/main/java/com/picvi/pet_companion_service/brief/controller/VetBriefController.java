package com.picvi.pet_companion_service.brief.controller;

import com.picvi.pet_companion_service.brief.dto.BriefOptionsResponse;
import com.picvi.pet_companion_service.brief.dto.VetBriefResponse;
import com.picvi.pet_companion_service.brief.service.VetBriefService;
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
@RequestMapping("/pets/{petId}/vet-brief")
public class VetBriefController {
    private final VetBriefService briefService;

    @GetMapping("/options")
    public BriefOptionsResponse options(@AuthenticationPrincipal UUID userId,
                                        @PathVariable UUID petId,
                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return this.briefService.options(userId, petId, date);
    }

    @GetMapping
    public VetBriefResponse brief(@AuthenticationPrincipal UUID userId,
                                  @PathVariable UUID petId,
                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                  @RequestParam(required = false) String reason,
                                  @RequestParam(required = false) String concern,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return this.briefService.brief(userId, petId, date, reason, concern, from, to);
    }
}
