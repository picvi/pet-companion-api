package com.picvi.pet_companion_service.breeds.controller;

import com.picvi.pet_companion_service.breeds.dto.BreedsResponse;
import com.picvi.pet_companion_service.breeds.service.BreedsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/breeds")
public class BreedsController {
    private final BreedsService breedsService;

    @GetMapping
    public BreedsResponse getAllBreeds() {
        return new BreedsResponse(this.breedsService.getAllBreeds());
    }
}
