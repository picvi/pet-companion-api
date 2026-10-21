package com.picvi.pet_companion_service.nutrition.controller;

import com.picvi.pet_companion_service.nutrition.dto.TriedFoodDto;
import com.picvi.pet_companion_service.nutrition.dto.TriedFoodRequest;
import com.picvi.pet_companion_service.nutrition.dto.TriedFoodsResponse;
import com.picvi.pet_companion_service.nutrition.service.TriedFoodService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/pets/{petId}/tried-foods")
public class TriedFoodController {
    private final TriedFoodService triedFoodService;

    @GetMapping
    public TriedFoodsResponse list(@AuthenticationPrincipal UUID userId, @PathVariable UUID petId) {
        return this.triedFoodService.list(userId, petId);
    }

    @PutMapping("/{foodCode}")
    public TriedFoodDto mark(@AuthenticationPrincipal UUID userId,
                             @PathVariable UUID petId,
                             @PathVariable String foodCode,
                             @Valid @RequestBody TriedFoodRequest request) {
        return this.triedFoodService.mark(userId, petId, foodCode, request);
    }

    @DeleteMapping("/{foodCode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@AuthenticationPrincipal UUID userId, @PathVariable UUID petId, @PathVariable String foodCode) {
        this.triedFoodService.remove(userId, petId, foodCode);
    }
}
