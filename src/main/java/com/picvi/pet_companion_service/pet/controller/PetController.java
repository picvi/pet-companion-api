package com.picvi.pet_companion_service.pet.controller;

import com.picvi.pet_companion_service.pet.dto.AddPetRequest;
import com.picvi.pet_companion_service.pet.dto.DismissVaccinationPromptRequest;
import com.picvi.pet_companion_service.pet.dto.PetResponse;
import com.picvi.pet_companion_service.pet.dto.UpdatePetRequest;
import com.picvi.pet_companion_service.pet.dto.UpdateVaccinationStatusRequest;
import com.picvi.pet_companion_service.pet.service.PetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/pets")
public class PetController {
    private final PetService petService;

    @PostMapping
    public PetResponse addPet(@AuthenticationPrincipal UUID userId, @RequestBody AddPetRequest request) {
        return this.petService.addPet(userId, request);
    }

    @GetMapping("/me")
    public PetResponse getCurrentPet(@AuthenticationPrincipal UUID userId) {
        return this.petService.getCurrentPet(userId);
    }

    @GetMapping("/{petId}")
    public PetResponse getPet(@AuthenticationPrincipal UUID userId, @PathVariable UUID petId) {
        return this.petService.getPetById(userId, petId);
    }

    @PutMapping("/{petId}")
    public PetResponse updatePet(@AuthenticationPrincipal UUID userId,
                                 @PathVariable UUID petId,
                                 @Valid @RequestBody UpdatePetRequest request) {
        return this.petService.updatePet(userId, petId, request);
    }

    @PutMapping("/{petId}/vaccination-status")
    public PetResponse updateVaccinationStatus(@AuthenticationPrincipal UUID userId,
                                               @PathVariable UUID petId,
                                               @Valid @RequestBody UpdateVaccinationStatusRequest request) {
        return this.petService.updateVaccinationStatus(userId, petId, request.status(), request.date());
    }

    @PostMapping("/{petId}/vaccination-prompt/dismiss")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void dismissVaccinationPrompt(@AuthenticationPrincipal UUID userId,
                                         @PathVariable UUID petId,
                                         @RequestBody DismissVaccinationPromptRequest request) {
        this.petService.dismissVaccinationPrompt(userId, petId, request.date());
    }
}
