package com.picvi.pet_companion_service.lostdog.controller;

import com.picvi.pet_companion_service.lostdog.domain.LostStatus;
import com.picvi.pet_companion_service.lostdog.domain.LostStepCode;
import com.picvi.pet_companion_service.lostdog.dto.*;
import com.picvi.pet_companion_service.lostdog.exception.InvalidLostReport;
import com.picvi.pet_companion_service.lostdog.service.LostDogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/pets/{petId}")
public class LostDogController {
    private final LostDogService service;

    @GetMapping("/lost-profile")
    public LostProfileResponse profile(@AuthenticationPrincipal UUID ownerId, @PathVariable UUID petId) {
        return this.service.profile(ownerId, petId);
    }

    @PutMapping("/lost-profile")
    public LostProfileResponse saveProfile(@AuthenticationPrincipal UUID ownerId, @PathVariable UUID petId,
                                           @Valid @RequestBody SaveLostProfileRequest request) {
        return this.service.saveProfile(ownerId, petId, request);
    }

    @GetMapping("/lost-reports/active")
    public ResponseEntity<LostReportDto> active(@AuthenticationPrincipal UUID ownerId, @PathVariable UUID petId) {
        return this.service.active(ownerId, petId).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/lost-reports")
    public ResponseEntity<LostReportDto> start(@AuthenticationPrincipal UUID ownerId, @PathVariable UUID petId,
                                               @Valid @RequestBody StartLostReportRequest request) {
        LostDogService.CreateResult result = this.service.start(ownerId, petId, request);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(result.report());
    }

    @PutMapping("/lost-reports/{reportId}/last-seen")
    public LostReportDto updateLastSeen(@AuthenticationPrincipal UUID ownerId, @PathVariable UUID petId,
                                        @PathVariable UUID reportId, @RequestHeader("If-Match") String ifMatch,
                                        @Valid @RequestBody UpdateLostSeenRequest request) {
        return this.service.updateLastSeen(ownerId, petId, reportId, parseVersion(ifMatch), request);
    }

    @PutMapping("/lost-reports/{reportId}/steps/{code}")
    public LostReportDto setStep(@AuthenticationPrincipal UUID ownerId, @PathVariable UUID petId,
                                 @PathVariable UUID reportId, @PathVariable LostStepCode code,
                                 @RequestHeader("If-Match") String ifMatch,
                                 @Valid @RequestBody SetLostStepRequest request) {
        return this.service.setStep(ownerId, petId, reportId, parseVersion(ifMatch), code, request.done());
    }

    @PostMapping("/lost-reports/{reportId}/found")
    public LostReportDto found(@AuthenticationPrincipal UUID ownerId, @PathVariable UUID petId,
                               @PathVariable UUID reportId, @RequestHeader("If-Match") String ifMatch) {
        return this.service.close(ownerId, petId, reportId, parseVersion(ifMatch), LostStatus.FOUND);
    }

    @PostMapping("/lost-reports/{reportId}/stop")
    public LostReportDto stop(@AuthenticationPrincipal UUID ownerId, @PathVariable UUID petId,
                              @PathVariable UUID reportId, @RequestHeader("If-Match") String ifMatch) {
        return this.service.close(ownerId, petId, reportId, parseVersion(ifMatch), LostStatus.STOPPED);
    }

    private static long parseVersion(String value) {
        try {
            return Long.parseLong(value.replace("W/", "").replace("\"", "").trim());
        } catch (NumberFormatException exception) {
            throw new InvalidLostReport("If-Match must be the lost report version");
        }
    }
}
