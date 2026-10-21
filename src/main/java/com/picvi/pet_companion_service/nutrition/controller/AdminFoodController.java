package com.picvi.pet_companion_service.nutrition.controller;

import com.picvi.pet_companion_service.nutrition.dto.ApproveFoodProductRequest;
import com.picvi.pet_companion_service.nutrition.dto.FoodProductDto;
import com.picvi.pet_companion_service.nutrition.dto.PendingFoodGroupDto;
import com.picvi.pet_companion_service.nutrition.service.AdminFoodService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Admin only (see SecurityConfig). Used from Swagger UI; there is no admin screen. */
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin/food-products")
public class AdminFoodController {
    private final AdminFoodService adminFoodService;

    @GetMapping("/pending")
    public List<PendingFoodGroupDto> pending() {
        return this.adminFoodService.pending();
    }

    @PostMapping("/{productId}/approve")
    public FoodProductDto approve(@AuthenticationPrincipal UUID adminId,
                                  @PathVariable UUID productId,
                                  @Valid @RequestBody ApproveFoodProductRequest request) {
        return this.adminFoodService.approve(adminId, productId, request);
    }

    @PostMapping("/{productId}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reject(@AuthenticationPrincipal UUID adminId, @PathVariable UUID productId) {
        this.adminFoodService.reject(adminId, productId);
    }
}
