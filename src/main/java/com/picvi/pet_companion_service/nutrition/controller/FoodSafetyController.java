package com.picvi.pet_companion_service.nutrition.controller;

import com.picvi.pet_companion_service.common.Etags;
import com.picvi.pet_companion_service.nutrition.dto.FoodSafetyResponse;
import com.picvi.pet_companion_service.nutrition.service.FoodSafetyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import java.time.Duration;

@RequiredArgsConstructor
@RestController
public class FoodSafetyController {
    private final FoodSafetyService service;

    @GetMapping("/reference/food-safety")
    public ResponseEntity<FoodSafetyResponse> foodSafety(WebRequest request) {
        FoodSafetyResponse body = this.service.catalogue();
        String etag = Etags.of(body);
        if (request.checkNotModified(etag)) {
            return null;
        }
        return ResponseEntity.ok()
                .eTag(etag)
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).mustRevalidate())
                .body(body);
    }
}
