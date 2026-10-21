package com.picvi.pet_companion_service.nutrition.controller;

import com.picvi.pet_companion_service.nutrition.domain.TransitionPace;
import com.picvi.pet_companion_service.nutrition.dto.DietResponse;
import com.picvi.pet_companion_service.nutrition.dto.FoodChangeActionRequest;
import com.picvi.pet_companion_service.nutrition.dto.FoodChangePreviewResponse;
import com.picvi.pet_companion_service.nutrition.dto.FoodDateRequest;
import com.picvi.pet_companion_service.nutrition.dto.StartFoodChangeRequest;
import com.picvi.pet_companion_service.nutrition.service.FoodTransitionService;
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
@RequestMapping("/pets/{petId}/food-transitions")
public class FoodTransitionController {
    private final FoodTransitionService transitionService;

    @GetMapping("/preview")
    public FoodChangePreviewResponse preview(@AuthenticationPrincipal UUID userId,
                                             @PathVariable UUID petId,
                                             @RequestParam UUID productId,
                                             @RequestParam(required = false) UUID fromFoodId,
                                             @RequestParam TransitionPace pace,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startOn,
                                             @RequestParam(required = false) Integer dailyGrams,
                                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return this.transitionService.preview(userId, petId, productId, fromFoodId, pace, startOn, dailyGrams, date);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DietResponse start(@AuthenticationPrincipal UUID userId,
                              @PathVariable UUID petId,
                              @Valid @RequestBody StartFoodChangeRequest request) {
        return this.transitionService.start(userId, petId, request);
    }

    @PostMapping("/{transitionId}/actions")
    public DietResponse act(@AuthenticationPrincipal UUID userId,
                            @PathVariable UUID petId,
                            @PathVariable UUID transitionId,
                            @Valid @RequestBody FoodChangeActionRequest request) {
        return this.transitionService.act(userId, petId, transitionId, request);
    }

    @PostMapping("/{transitionId}/seen")
    public DietResponse seen(@AuthenticationPrincipal UUID userId,
                             @PathVariable UUID petId,
                             @PathVariable UUID transitionId,
                             @Valid @RequestBody FoodDateRequest request) {
        return this.transitionService.markSeen(userId, petId, transitionId, request.date());
    }
}
