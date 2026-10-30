package com.picvi.pet_companion_service.nutrition.controller;

import com.picvi.pet_companion_service.common.Etags;
import com.picvi.pet_companion_service.nutrition.domain.FoodForm;
import com.picvi.pet_companion_service.nutrition.domain.FoodKind;
import com.picvi.pet_companion_service.nutrition.domain.LifeStage;
import com.picvi.pet_companion_service.nutrition.dto.AddFoodProductRequest;
import com.picvi.pet_companion_service.nutrition.dto.FoodProductDto;
import com.picvi.pet_companion_service.nutrition.dto.FoodProductsResponse;
import com.picvi.pet_companion_service.nutrition.dto.IngredientSourcesResponse;
import com.picvi.pet_companion_service.nutrition.service.FoodCatalogueService;
import com.picvi.pet_companion_service.nutrition.service.IngredientSourceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

import java.time.Duration;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
public class FoodCatalogueController {
    private final FoodCatalogueService catalogueService;
    private final IngredientSourceService sourceService;

    @GetMapping("/food-products")
    public FoodProductsResponse search(@AuthenticationPrincipal UUID userId,
                                       @RequestParam(required = false, defaultValue = "") String q,
                                       @RequestParam(required = false) FoodKind kind,
                                       @RequestParam(required = false) FoodForm form,
                                       @RequestParam(required = false) LifeStage lifeStage,
                                       @RequestParam(required = false) String cursor,
                                       @RequestParam(required = false) Integer limit) {
        return this.catalogueService.search(userId, q, kind, form, lifeStage, cursor, limit);
    }

    @PostMapping("/food-products")
    @ResponseStatus(HttpStatus.CREATED)
    public FoodProductDto add(@AuthenticationPrincipal UUID userId, @Valid @RequestBody AddFoodProductRequest request) {
        return this.catalogueService.addOwnProduct(userId, request);
    }

    @GetMapping("/reference/ingredient-sources")
    public ResponseEntity<IngredientSourcesResponse> ingredientSources(WebRequest request) {
        IngredientSourcesResponse body = this.sourceService.list();
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
