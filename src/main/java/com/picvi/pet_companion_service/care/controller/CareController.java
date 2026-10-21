package com.picvi.pet_companion_service.care.controller;

import com.picvi.pet_companion_service.care.dto.CareEventDto;
import com.picvi.pet_companion_service.care.dto.CareEventsResponse;
import com.picvi.pet_companion_service.care.dto.CareItemDto;
import com.picvi.pet_companion_service.care.dto.CareScheduleResponse;
import com.picvi.pet_companion_service.care.dto.LogCareEventRequest;
import com.picvi.pet_companion_service.care.dto.UpdateCareSettingsRequest;
import com.picvi.pet_companion_service.care.service.CareEventService;
import com.picvi.pet_companion_service.care.service.CareScheduleService;
import com.picvi.pet_companion_service.care.service.CareSettingService;
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
@RequestMapping("/pets/{petId}/care")
public class CareController {
    private final CareScheduleService scheduleService;
    private final CareEventService eventService;
    private final CareSettingService settingService;

    @GetMapping
    public CareScheduleResponse getSchedule(@AuthenticationPrincipal UUID userId,
                                            @PathVariable UUID petId,
                                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return this.scheduleService.getSchedule(userId, petId, date);
    }

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.CREATED)
    public CareEventDto logEvent(@AuthenticationPrincipal UUID userId,
                                 @PathVariable UUID petId,
                                 @Valid @RequestBody LogCareEventRequest request) {
        return this.eventService.log(userId, petId, request.itemCode(), request.performedOn(), request.date());
    }

    @GetMapping("/events")
    public CareEventsResponse getHistory(@AuthenticationPrincipal UUID userId,
                                         @PathVariable UUID petId,
                                         @RequestParam String itemCode) {
        return this.eventService.history(userId, petId, itemCode);
    }

    @DeleteMapping("/events/{eventId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void undoEvent(@AuthenticationPrincipal UUID userId,
                          @PathVariable UUID petId,
                          @PathVariable UUID eventId,
                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        this.eventService.undo(userId, petId, eventId, date);
    }

    @PutMapping("/items/{code}/settings")
    public CareItemDto updateSettings(@AuthenticationPrincipal UUID userId,
                                      @PathVariable UUID petId,
                                      @PathVariable String code,
                                      @Valid @RequestBody UpdateCareSettingsRequest request) {
        return this.settingService.update(userId, petId, code, request.remindersEnabled(),
                request.pausedUntil(), request.intervalDays(), request.date());
    }
}
