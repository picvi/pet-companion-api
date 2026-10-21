package com.picvi.pet_companion_service.dayevent.controller;

import com.picvi.pet_companion_service.dayevent.dto.DayEventDto;
import com.picvi.pet_companion_service.dayevent.dto.DayEventRequest;
import com.picvi.pet_companion_service.dayevent.dto.DayEventsResponse;
import com.picvi.pet_companion_service.dayevent.dto.OccurrenceChangeRequest;
import com.picvi.pet_companion_service.dayevent.service.DayEventService;
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
@RequestMapping("/pets/{petId}/day-events")
public class DayEventController {
    private final DayEventService eventService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DayEventDto create(@AuthenticationPrincipal UUID userId,
                              @PathVariable UUID petId,
                              @Valid @RequestBody DayEventRequest request) {
        return this.eventService.create(userId, petId, request.type(), request.title(), request.localDate(),
                request.startMinute(), request.durationMinutes(), request.recurrence(), request.planAround(), request.date());
    }

    @PutMapping("/{eventId}")
    public DayEventDto update(@AuthenticationPrincipal UUID userId,
                              @PathVariable UUID petId,
                              @PathVariable UUID eventId,
                              @Valid @RequestBody DayEventRequest request) {
        return this.eventService.update(userId, petId, eventId, request.type(), request.title(), request.localDate(),
                request.startMinute(), request.durationMinutes(), request.recurrence(), request.planAround(), request.date());
    }

    @DeleteMapping("/{eventId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UUID userId,
                       @PathVariable UUID petId,
                       @PathVariable UUID eventId,
                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        this.eventService.delete(userId, petId, eventId, date);
    }

    @PutMapping("/{eventId}/occurrences/{occurrenceDate}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeOccurrence(@AuthenticationPrincipal UUID userId,
                                 @PathVariable UUID petId,
                                 @PathVariable UUID eventId,
                                 @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate occurrenceDate,
                                 @Valid @RequestBody OccurrenceChangeRequest request) {
        this.eventService.changeOccurrence(userId, petId, eventId, occurrenceDate, request.skipped(), request.startMinute(), request.date());
    }

    @GetMapping
    public DayEventsResponse list(@AuthenticationPrincipal UUID userId,
                                  @PathVariable UUID petId,
                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return this.eventService.list(userId, petId, date);
    }
}
