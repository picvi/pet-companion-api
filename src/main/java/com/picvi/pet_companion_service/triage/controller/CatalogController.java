package com.picvi.pet_companion_service.triage.controller;

import com.picvi.pet_companion_service.triage.dto.CatalogResponse;
import com.picvi.pet_companion_service.triage.service.CatalogLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequiredArgsConstructor
@RequestMapping("/triage/catalog")
public class CatalogController {
    private final CatalogLoader catalogLoader;

    @GetMapping
    public ResponseEntity<CatalogResponse> get() {
        CatalogResponse response = CatalogResponse.of(this.catalogLoader.getCatalog());
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .eTag('"' + response.version() + '"').body(response);
    }
}
