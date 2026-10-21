package com.picvi.pet_companion_service.medical.controller;

import com.picvi.pet_companion_service.exception.nutrition.InvalidPetFood;
import com.picvi.pet_companion_service.medical.dto.CreateVetVisitRequest;
import com.picvi.pet_companion_service.medical.dto.UpdateVetVisitRequest;
import com.picvi.pet_companion_service.medical.dto.VetVisitDto;
import com.picvi.pet_companion_service.medical.dto.VetVisitListResponse;
import com.picvi.pet_companion_service.medical.service.VetVisitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/pets/{petId}/vet-visits")
public class VetVisitController {
    private final VetVisitService visitService;

    @GetMapping
    public VetVisitListResponse list(@AuthenticationPrincipal UUID userId,
                                     @PathVariable UUID petId,
                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return this.visitService.list(userId, petId, date);
    }

    @PostMapping
    public ResponseEntity<VetVisitDto> create(@AuthenticationPrincipal UUID userId,
                                              @PathVariable UUID petId,
                                              @Valid @RequestBody CreateVetVisitRequest request) {
        VetVisitService.CreateResult result = this.visitService.create(userId, petId, request);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(result.visit());
    }

    @GetMapping("/{visitId}")
    public VetVisitDto get(@AuthenticationPrincipal UUID userId,
                           @PathVariable UUID petId,
                           @PathVariable UUID visitId) {
        return this.visitService.get(userId, petId, visitId);
    }

    @PutMapping("/{visitId}")
    public VetVisitDto update(@AuthenticationPrincipal UUID userId,
                              @PathVariable UUID petId,
                              @PathVariable UUID visitId,
                              @RequestHeader("If-Match") String ifMatch,
                              @Valid @RequestBody UpdateVetVisitRequest request) {
        return this.visitService.update(userId, petId, visitId, parseVersion(ifMatch), request);
    }

    @DeleteMapping("/{visitId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UUID userId,
                       @PathVariable UUID petId,
                       @PathVariable UUID visitId) {
        this.visitService.delete(userId, petId, visitId);
    }

    private static long parseVersion(String ifMatch) {
        try {
            return Long.parseLong(ifMatch.replace("W/", "").replace("\"", "").trim());
        } catch (NumberFormatException e) {
            throw new InvalidPetFood("If-Match must be the vet visit's version");
        }
    }
}
