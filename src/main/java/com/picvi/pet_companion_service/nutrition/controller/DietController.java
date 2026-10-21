package com.picvi.pet_companion_service.nutrition.controller;

import com.picvi.pet_companion_service.exception.nutrition.InvalidPetFood;
import com.picvi.pet_companion_service.nutrition.domain.NoticeCode;
import com.picvi.pet_companion_service.nutrition.dto.AddPetFoodRequest;
import com.picvi.pet_companion_service.nutrition.dto.DietHistoryResponse;
import com.picvi.pet_companion_service.nutrition.dto.DietResponse;
import com.picvi.pet_companion_service.nutrition.dto.FoodDateRequest;
import com.picvi.pet_companion_service.nutrition.dto.UpdatePetFoodRequest;
import com.picvi.pet_companion_service.nutrition.service.DietHistoryService;
import com.picvi.pet_companion_service.nutrition.service.DietService;
import com.picvi.pet_companion_service.pet.service.PetService;
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
@RequestMapping("/pets/{petId}")
public class DietController {
    private final DietService dietService;
    private final DietHistoryService historyService;
    private final PetService petService;

    @GetMapping("/diet")
    public DietResponse getDiet(@AuthenticationPrincipal UUID userId,
                                @PathVariable UUID petId,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return this.dietService.getDiet(userId, petId, date);
    }

    @GetMapping("/diet/history")
    public DietHistoryResponse history(@AuthenticationPrincipal UUID userId,
                                       @PathVariable UUID petId,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        this.petService.getOwnedPetProfile(userId, petId);
        return this.historyService.history(petId, from, to);
    }

    @PostMapping("/foods")
    @ResponseStatus(HttpStatus.CREATED)
    public DietResponse addFood(@AuthenticationPrincipal UUID userId,
                                @PathVariable UUID petId,
                                @Valid @RequestBody AddPetFoodRequest request) {
        return this.dietService.addFood(userId, petId, request);
    }

    @PutMapping("/foods/{foodId}")
    public DietResponse updateFood(@AuthenticationPrincipal UUID userId,
                                   @PathVariable UUID petId,
                                   @PathVariable UUID foodId,
                                   @RequestHeader("If-Match") String ifMatch,
                                   @Valid @RequestBody UpdatePetFoodRequest request) {
        return this.dietService.updateFood(userId, petId, foodId, parseVersion(ifMatch), request);
    }

    @PostMapping("/foods/{foodId}/end")
    public DietResponse endFood(@AuthenticationPrincipal UUID userId,
                                @PathVariable UUID petId,
                                @PathVariable UUID foodId,
                                @Valid @RequestBody FoodDateRequest request) {
        return this.dietService.endFood(userId, petId, foodId, request.date());
    }

    @PostMapping("/foods/{foodId}/remove")
    public DietResponse removeFood(@AuthenticationPrincipal UUID userId,
                                   @PathVariable UUID petId,
                                   @PathVariable UUID foodId,
                                   @Valid @RequestBody FoodDateRequest request) {
        return this.dietService.removeFood(userId, petId, foodId, request.date());
    }

    @PostMapping("/diet/notices/{code}")
    public DietResponse dismissNotice(@AuthenticationPrincipal UUID userId,
                                      @PathVariable UUID petId,
                                      @PathVariable NoticeCode code,
                                      @Valid @RequestBody FoodDateRequest request) {
        return this.dietService.dismissNotice(userId, petId, code, request.date());
    }

    /** The food's version, bare or as an ETag ("3"). */
    private static long parseVersion(String ifMatch) {
        try {
            return Long.parseLong(ifMatch.replace("W/", "").replace("\"", "").trim());
        } catch (NumberFormatException e) {
            throw new InvalidPetFood("If-Match must be the food's version");
        }
    }
}
