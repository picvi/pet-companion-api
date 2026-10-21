package com.picvi.pet_companion_service.pet.dto;

import com.picvi.pet_companion_service.pet.entity.Sex;
import com.picvi.pet_companion_service.pet.entity.VaccinationStatus;

import java.math.BigInteger;
import java.time.LocalDate;

public record AddPetRequest(
        BigInteger breedId,
        String name,
        Sex sex,
        LocalDate birthDate,
        Boolean neutered,
        String microchipNumber,
        VaccinationStatus vaccinationStatus) {
}
