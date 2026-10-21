package com.picvi.pet_companion_service.pet.service;

import com.picvi.pet_companion_service.breeds.entity.Breed;
import com.picvi.pet_companion_service.breeds.service.BreedsService;
import com.picvi.pet_companion_service.common.ClientDateValidator;
import com.picvi.pet_companion_service.exception.UserDoesNotExist;
import com.picvi.pet_companion_service.exception.pet.InvalidBirthDate;
import com.picvi.pet_companion_service.exception.pet.PetNotFound;
import com.picvi.pet_companion_service.pet.domain.MicrochipNumber;
import com.picvi.pet_companion_service.pet.domain.VaccinationPrompt;
import com.picvi.pet_companion_service.pet.dto.AddPetRequest;
import com.picvi.pet_companion_service.pet.dto.PetProfile;
import com.picvi.pet_companion_service.pet.dto.PetResponse;
import com.picvi.pet_companion_service.pet.dto.UpdatePetRequest;
import com.picvi.pet_companion_service.pet.entity.Pet;
import com.picvi.pet_companion_service.pet.entity.VaccinationStatus;
import com.picvi.pet_companion_service.pet.repository.PetRepository;
import com.picvi.pet_companion_service.user.entity.User;
import com.picvi.pet_companion_service.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PetService {
    private final BreedsService breedsService;
    private final UserService userService;
    private final PetRepository petRepository;
    private final Clock clock;
    private final ClientDateValidator clientDateValidator;
    private final VaccinationPrompt vaccinationPrompt;

    public PetResponse addPet(UUID ownerId, AddPetRequest request) {
        validateBirthDate(request.birthDate());
        Breed breed = this.breedsService.getBreedById(request.breedId());
        User owner = this.userService.getUserByUUID(ownerId).orElseThrow(UserDoesNotExist::new);

        Pet newPet = new Pet();
        newPet.setBreed(breed);
        newPet.setOwner(owner);
        newPet.setName(request.name());
        newPet.setSex(request.sex());
        newPet.setBirthDate(request.birthDate());
        newPet.setNeutered(request.neutered());
        newPet.setMicrochipNumber(MicrochipNumber.normalise(request.microchipNumber()));
        newPet.setVaccinationStatus(request.vaccinationStatus() == null
                ? VaccinationStatus.UNKNOWN
                : request.vaccinationStatus());

        return this.toResponse(this.petRepository.save(newPet));
    }

    public PetResponse updatePet(UUID ownerId, UUID petId, UpdatePetRequest request) {
        this.validateBirthDate(request.birthDate());
        Pet pet = this.findOwnedPet(ownerId, petId);
        pet.setName(request.name().trim());
        pet.setSex(request.sex());
        pet.setBirthDate(request.birthDate());
        pet.setNeutered(request.neutered());
        pet.setMicrochipNumber(MicrochipNumber.normalise(request.microchipNumber()));
        pet.setBreed(this.breedsService.getBreedById(request.breedId()));
        return this.toResponse(this.petRepository.save(pet));
    }

    public int deleteAllOwnedBy(UUID ownerId) {
        return this.petRepository.deleteAllByOwnerId(ownerId);
    }

    public PetResponse getCurrentPet(UUID ownerId) {
        return this.petRepository.findFirstByOwnerId(ownerId)
                .map(this::toResponse)
                .orElseThrow(PetNotFound::new);
    }

    public PetResponse getPetById(UUID ownerId, UUID petId) {
        return this.toResponse(this.findOwnedPet(ownerId, petId));
    }

    public PetProfile getOwnedPetProfile(UUID ownerId, UUID petId) {
        return this.toProfile(this.findOwnedPet(ownerId, petId));
    }

    public PetResponse updateVaccinationStatus(UUID ownerId, UUID petId, VaccinationStatus status, LocalDate date) {
        this.clientDateValidator.validate(date);
        Pet pet = this.findOwnedPet(ownerId, petId);
        boolean promptWasDue = this.isVaccinationPromptDue(pet, date);

        if (pet.getVaccinationStatus() != status) {
            pet.setVaccinationStatus(status);
            pet.setVaccinationStatusChangedOn(date);
        }
        if (promptWasDue && status != VaccinationStatus.SERIES_COMPLETE) {
            pet.setVaccinationPromptCount(pet.getVaccinationPromptCount() + 1);
        }

        return this.toResponse(this.petRepository.save(pet));
    }

    public void raiseVaccinationStatus(UUID petId, VaccinationStatus minimum, LocalDate date) {
        Pet pet = this.petRepository.findById(petId).orElseThrow(PetNotFound::new);

        if (pet.getVaccinationStatus().isLowerThan(minimum)) {
            pet.setVaccinationStatus(minimum);
            pet.setVaccinationStatusChangedOn(date);
            this.petRepository.save(pet);
        }
    }

    public void dismissVaccinationPrompt(UUID ownerId, UUID petId, LocalDate date) {
        this.clientDateValidator.validate(date);
        Pet pet = this.findOwnedPet(ownerId, petId);

        if (this.isVaccinationPromptDue(pet, date)) {
            pet.setVaccinationPromptCount(pet.getVaccinationPromptCount() + 1);
            this.petRepository.save(pet);
        }
    }

    public Pet getReference(UUID petId) {
        return this.petRepository.getReferenceById(petId);
    }

    private Pet findOwnedPet(UUID ownerId, UUID petId) {
        return this.petRepository.findByIdAndOwnerId(petId, ownerId).orElseThrow(PetNotFound::new);
    }

    private boolean isVaccinationPromptDue(Pet pet, LocalDate date) {
        int ageWeeks = (int) (ChronoUnit.DAYS.between(pet.getBirthDate(), date) / 7);
        return this.vaccinationPrompt.isDue(pet.getVaccinationStatus(), pet.getVaccinationPromptCount(), ageWeeks);
    }

    private void validateBirthDate(LocalDate birthDate) {
        if (birthDate == null || birthDate.isAfter(LocalDate.now(this.clock))) {
            throw new InvalidBirthDate();
        }
    }

    private PetProfile toProfile(Pet pet) {
        return new PetProfile(
                pet.getId(),
                pet.getName(),
                pet.getBirthDate(),
                pet.getBreed().getSizeCategory(),
                pet.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate(),
                pet.getVaccinationStatus(),
                pet.getVaccinationStatusChangedOn(),
                pet.getVaccinationPromptCount()
        );
    }

    private PetResponse toResponse(Pet pet) {
        return new PetResponse(
                pet.getId(),
                pet.getName(),
                pet.getSex(),
                pet.getBirthDate(),
                pet.getNeutered(),
                pet.getMicrochipNumber(),
                pet.getOwner().getId(),
                pet.getBreed().getId(),
                pet.getBreed().getCode(),
                pet.getVaccinationStatus(),
                pet.getBreed().getSizeCategory()
        );
    }
}
