package com.picvi.pet_companion_service.development.controller;

import com.picvi.pet_companion_service.development.dto.DevelopmentResponse;
import com.picvi.pet_companion_service.development.dto.TodayResponse;
import com.picvi.pet_companion_service.development.service.DevelopmentService;
import com.picvi.pet_companion_service.development.service.TodayService;
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
@RequestMapping("/pets/{petId}")
public class DevelopmentController {
    private final DevelopmentService developmentService;
    private final TodayService todayService;

    @GetMapping("/development")
    public DevelopmentResponse getDevelopment(@AuthenticationPrincipal UUID userId,
                                              @PathVariable UUID petId,
                                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return this.developmentService.getDevelopment(userId, petId, date);
    }

    @GetMapping("/today")
    public TodayResponse getToday(@AuthenticationPrincipal UUID userId,
                                  @PathVariable UUID petId,
                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return this.todayService.getToday(userId, petId, date);
    }
}
