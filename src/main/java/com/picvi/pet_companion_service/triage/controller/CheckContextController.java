package com.picvi.pet_companion_service.triage.controller;

import com.picvi.pet_companion_service.triage.dto.CheckContextResponse;
import com.picvi.pet_companion_service.triage.dto.CheckResponse;
import com.picvi.pet_companion_service.triage.dto.OpenChecksResponse;
import com.picvi.pet_companion_service.triage.service.CheckReadService;
import com.picvi.pet_companion_service.triage.service.CheckContextService;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/pets/{petId}/checks")
public class CheckContextController {
    private final CheckContextService contextService;
    private final CheckReadService readService;

    @GetMapping("/context")
    public CheckContextResponse context(@AuthenticationPrincipal UUID ownerId,
                                        @PathVariable UUID petId,
                                        @RequestParam String item,
                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return this.contextService.get(ownerId, petId, item, date);
    }

    @GetMapping("/open")
    public OpenChecksResponse open(@AuthenticationPrincipal UUID ownerId,
                                   @PathVariable UUID petId,
                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return this.readService.open(ownerId, petId, date);
    }

    @GetMapping("/{checkId}")
    public CheckResponse get(@AuthenticationPrincipal UUID ownerId,
                             @PathVariable UUID petId,
                             @PathVariable UUID checkId) {
        return this.readService.get(ownerId, petId, checkId);
    }
}
