package com.picvi.pet_companion_service.pet.dto;

import com.picvi.pet_companion_service.pet.entity.Sex;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigInteger;
import java.time.LocalDate;

public record UpdatePetRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull Sex sex,
        @NotNull LocalDate birthDate,
        @NotNull BigInteger breedId,
        @NotNull Boolean neutered,
        String microchipNumber) {
}
