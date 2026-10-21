package com.picvi.pet_companion_service.pet.dto;

import com.picvi.pet_companion_service.breeds.entity.SizeCategory;
import com.picvi.pet_companion_service.pet.entity.Sex;
import com.picvi.pet_companion_service.pet.entity.VaccinationStatus;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.UUID;

public record PetResponse(UUID id,
                          String name,
                          Sex sex,
                          LocalDate birthDate,
                          Boolean neutered,
                          String microchipNumber,
                          UUID ownerId,
                          BigInteger breedId,
                          String breedCode,
                          VaccinationStatus vaccinationStatus,
                          SizeCategory sizeCategory) {
}
