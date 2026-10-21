package com.picvi.pet_companion_service.petlog.controller;

import com.picvi.pet_companion_service.petlog.dto.CreatePetLogRequest;
import com.picvi.pet_companion_service.petlog.domain.LogType;
import com.picvi.pet_companion_service.petlog.dto.PetLogDto;
import com.picvi.pet_companion_service.petlog.dto.PetLogHistoryResponse;
import com.picvi.pet_companion_service.petlog.dto.RecentLogsResponse;
import com.picvi.pet_companion_service.petlog.dto.PetLogsResponse;
import com.picvi.pet_companion_service.petlog.dto.UpdatePetLogRequest;
import com.picvi.pet_companion_service.petlog.service.PetLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/pets/{petId}/logs")
public class PetLogController {
    private final PetLogService logService;

    @PostMapping
    public ResponseEntity<PetLogDto> log(@AuthenticationPrincipal UUID userId,
                                         @PathVariable UUID petId,
                                         @Valid @RequestBody CreatePetLogRequest request) {
        PetLogService.LogResult result = this.logService.log(userId, petId, request.toNewLog(), request.date());
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(result.log());
    }

    @PutMapping("/{logId}")
    public PetLogDto update(@AuthenticationPrincipal UUID userId,
                            @PathVariable UUID petId,
                            @PathVariable UUID logId,
                            @Valid @RequestBody UpdatePetLogRequest request) {
        return this.logService.update(userId, petId, logId, request.toEdit(), request.date());
    }

    @GetMapping
    public PetLogsResponse list(@AuthenticationPrincipal UUID userId,
                                @PathVariable UUID petId,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return this.logService.list(userId, petId, date);
    }

    @GetMapping("/history")
    public PetLogHistoryResponse history(@AuthenticationPrincipal UUID userId,
                                         @PathVariable UUID petId,
                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                         @RequestParam(required = false) Set<LogType> types,
                                         @RequestParam(required = false) String before) {
        return this.logService.history(userId, petId, date, types, before);
    }

    @GetMapping("/recent")
    public RecentLogsResponse recent(@AuthenticationPrincipal UUID userId,
                                     @PathVariable UUID petId,
                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return this.logService.recent(userId, petId, date);
    }

    @DeleteMapping("/{logId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void undo(@AuthenticationPrincipal UUID userId,
                     @PathVariable UUID petId,
                     @PathVariable UUID logId,
                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        this.logService.undo(userId, petId, logId, date);
    }
}
