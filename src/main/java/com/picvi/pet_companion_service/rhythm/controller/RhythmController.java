package com.picvi.pet_companion_service.rhythm.controller;

import com.picvi.pet_companion_service.rhythm.domain.DaySettings;
import com.picvi.pet_companion_service.rhythm.dto.NoticeRequest;
import com.picvi.pet_companion_service.rhythm.dto.RhythmDayResponse;
import com.picvi.pet_companion_service.rhythm.dto.UpdateMealsPerDayRequest;
import com.picvi.pet_companion_service.rhythm.dto.UpdateRhythmSettingsRequest;
import com.picvi.pet_companion_service.rhythm.service.RhythmService;
import com.picvi.pet_companion_service.rhythm.service.RhythmSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/pets/{petId}/rhythm")
public class RhythmController {
    private final RhythmService rhythmService;
    private final RhythmSettingsService settingsService;

    @GetMapping
    public RhythmDayResponse getDay(@AuthenticationPrincipal UUID userId,
                                    @PathVariable UUID petId,
                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return this.rhythmService.getDay(userId, petId, date);
    }

    @PutMapping("/settings")
    public RhythmDayResponse updateSettings(@AuthenticationPrincipal UUID userId,
                                            @PathVariable UUID petId,
                                            @Valid @RequestBody UpdateRhythmSettingsRequest request) {
        DaySettings settings = new DaySettings(request.wakeMinute(), request.bedMinute(), request.mealMinutes(),
                request.customAwakeMinutes());
        return this.settingsService.update(userId, petId, settings, request.date());
    }

    @PutMapping("/meals-per-day")
    public RhythmDayResponse updateMealsPerDay(@AuthenticationPrincipal UUID userId,
                                               @PathVariable UUID petId,
                                               @Valid @RequestBody UpdateMealsPerDayRequest request) {
        return this.settingsService.updateMealsPerDay(userId, petId, request.mealsPerDay(), request.date());
    }

    @PostMapping("/band-note/seen")
    public RhythmDayResponse markBandNoteSeen(@AuthenticationPrincipal UUID userId,
                                              @PathVariable UUID petId,
                                              @RequestBody NoticeRequest request) {
        return this.settingsService.markBandNoteSeen(userId, petId, request.date());
    }

    @PostMapping("/checkin")
    public RhythmDayResponse answerCheckin(@AuthenticationPrincipal UUID userId,
                                           @PathVariable UUID petId,
                                           @RequestBody NoticeRequest request) {
        return this.settingsService.answerCheckin(userId, petId, request.date());
    }
}
