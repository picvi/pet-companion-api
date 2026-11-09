package com.picvi.pet_companion_service.medical.controller;

import com.picvi.pet_companion_service.common.Etags;
import com.picvi.pet_companion_service.exception.nutrition.InvalidPetFood;
import com.picvi.pet_companion_service.medical.dto.PlanVisitRequest;
import com.picvi.pet_companion_service.medical.dto.UpcomingVisitsResponse;
import com.picvi.pet_companion_service.medical.dto.VetVisitDto;
import com.picvi.pet_companion_service.medical.service.VetVisitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDate;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/pets/{petId}/vet-visits")
public class VetVisitController {
    private final VetVisitService visitService;

    @GetMapping("/upcoming")
    public ResponseEntity<UpcomingVisitsResponse> upcoming(@AuthenticationPrincipal UUID userId,
                                                           @PathVariable UUID petId,
                                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                                           WebRequest request) {
        UpcomingVisitsResponse body = this.visitService.upcoming(userId, petId, date);
        String etag = Etags.of(body);
        if (request.checkNotModified(etag)) {
            return null;
        }
        return ResponseEntity.ok().eTag(etag).cacheControl(CacheControl.noCache()).body(body);
    }

    @GetMapping("/{visitId}")
    public VetVisitDto get(@AuthenticationPrincipal UUID userId,
                           @PathVariable UUID petId,
                           @PathVariable UUID visitId) {
        return this.visitService.get(userId, petId, visitId);
    }

    @PostMapping("/plans")
    public ResponseEntity<VetVisitDto> plan(@AuthenticationPrincipal UUID userId,
                                            @PathVariable UUID petId,
                                            @Valid @RequestBody PlanVisitRequest request) {
        VetVisitService.CreateResult result = this.visitService.plan(userId, petId, request);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(result.visit());
    }

    @PutMapping("/{visitId}/plan")
    public VetVisitDto updatePlan(@AuthenticationPrincipal UUID userId,
                                  @PathVariable UUID petId,
                                  @PathVariable UUID visitId,
                                  @RequestHeader("If-Match") String ifMatch,
                                  @Valid @RequestBody PlanVisitRequest request) {
        return this.visitService.updatePlan(userId, petId, visitId, parseVersion(ifMatch), request);
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
