package com.picvi.pet_companion_service.triage.controller;

import com.picvi.pet_companion_service.triage.dto.CheckResponse;
import com.picvi.pet_companion_service.triage.dto.CreateCheckRequest;
import com.picvi.pet_companion_service.triage.dto.FollowUpRequest;
import com.picvi.pet_companion_service.triage.service.CheckFollowUpService;
import com.picvi.pet_companion_service.triage.service.CheckService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/pets/{petId}/checks")
public class CheckController {
    private final CheckService checkService;
    private final CheckFollowUpService followUpService;

    @PostMapping
    public ResponseEntity<CheckResponse> create(@AuthenticationPrincipal UUID ownerId,
                                                @PathVariable UUID petId,
                                                @Valid @RequestBody CreateCheckRequest request) {
        CheckService.CreateCheckResult result = this.checkService.create(ownerId, petId, request);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(result.response());
    }

    @PostMapping("/{checkId}/follow-ups")
    public CheckResponse followUp(@AuthenticationPrincipal UUID ownerId,
                                  @PathVariable UUID petId,
                                  @PathVariable UUID checkId,
                                  @Valid @RequestBody FollowUpRequest request) {
        return this.followUpService.answer(ownerId, petId, checkId, request);
    }

    @PostMapping("/{checkId}/dismiss")
    public CheckResponse dismiss(@AuthenticationPrincipal UUID ownerId,
                                 @PathVariable UUID petId,
                                 @PathVariable UUID checkId) {
        return this.followUpService.dismiss(ownerId, petId, checkId);
    }

    @DeleteMapping("/{checkId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UUID ownerId,
                                       @PathVariable UUID petId,
                                       @PathVariable UUID checkId) {
        this.checkService.delete(ownerId, petId, checkId);
        return ResponseEntity.noContent().build();
    }
}
