package com.picvi.pet_companion_service.timeline.controller;

import com.picvi.pet_companion_service.timeline.domain.TimelineFilter;
import com.picvi.pet_companion_service.timeline.dto.TimelineResponse;
import com.picvi.pet_companion_service.timeline.service.TimelineService;
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
@RequestMapping("/pets/{petId}/timeline")
public class TimelineController {
    private final TimelineService timelineService;

    @GetMapping
    public TimelineResponse timeline(@AuthenticationPrincipal UUID userId,
                                     @PathVariable UUID petId,
                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                     @RequestParam(defaultValue = "ALL") TimelineFilter filter,
                                     @RequestParam(required = false) String before) {
        return this.timelineService.page(userId, petId, date, filter, before);
    }
}
