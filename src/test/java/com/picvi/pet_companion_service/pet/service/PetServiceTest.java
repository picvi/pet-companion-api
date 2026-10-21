package com.picvi.pet_companion_service.pet.service;

import com.picvi.pet_companion_service.breeds.entity.Breed;
import com.picvi.pet_companion_service.breeds.entity.SizeCategory;
import com.picvi.pet_companion_service.breeds.service.BreedsService;
import com.picvi.pet_companion_service.common.ClientDateValidator;
import com.picvi.pet_companion_service.exception.InvalidDate;
import com.picvi.pet_companion_service.exception.breed.BreedNotFound;
import com.picvi.pet_companion_service.exception.pet.InvalidBirthDate;
import com.picvi.pet_companion_service.exception.pet.InvalidMicrochip;
import com.picvi.pet_companion_service.exception.pet.PetNotFound;
import com.picvi.pet_companion_service.pet.domain.VaccinationPrompt;
import com.picvi.pet_companion_service.pet.dto.AddPetRequest;
import com.picvi.pet_companion_service.pet.dto.PetProfile;
import com.picvi.pet_companion_service.pet.dto.PetResponse;
import com.picvi.pet_companion_service.pet.dto.UpdatePetRequest;
import com.picvi.pet_companion_service.pet.entity.Pet;
import com.picvi.pet_companion_service.pet.entity.Sex;
import com.picvi.pet_companion_service.pet.entity.VaccinationStatus;
import com.picvi.pet_companion_service.pet.repository.PetRepository;
import com.picvi.pet_companion_service.user.entity.User;
import com.picvi.pet_companion_service.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigInteger;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PetServiceTest {
    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final BigInteger BREED_ID = BigInteger.ONE;
    private static final UUID PET_ID = UUID.randomUUID();
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 13);
    private static final LocalDate BIRTH_DATE_AT_TEN_WEEKS = TODAY.minusWeeks(10);
    private static final LocalDate BIRTH_DATE_AT_SIXTEEN_WEEKS = TODAY.minusWeeks(16);

    @Mock
    private BreedsService breedsService;
    @Mock
    private UserService userService;
    @Mock
    private PetRepository petRepository;

    private PetService petService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-13T12:00:00Z"), ZoneOffset.UTC);
        petService = new PetService(breedsService, userService, petRepository, clock,
                new ClientDateValidator(clock), new VaccinationPrompt());
    }

    @Test
    void addPetStoresBirthDateAsGiven() {
        User owner = new User();
        ReflectionTestUtils.setField(owner, "id", OWNER_ID);
        Breed breed = new Breed();
        ReflectionTestUtils.setField(breed, "id", BREED_ID);
        ReflectionTestUtils.setField(breed, "code", "LABRADOR");
        when(breedsService.getBreedById(BREED_ID)).thenReturn(breed);
        when(userService.getUserByUUID(OWNER_ID)).thenReturn(Optional.of(owner));
        when(petRepository.save(any(Pet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PetResponse response = petService.addPet(OWNER_ID, requestWithBirthDate(LocalDate.of(2026, 8, 1)));

        assertThat(response.birthDate()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(response.breedCode()).isEqualTo("LABRADOR");
    }

    @Test
    void addPetAcceptsBirthDateOfToday() {
        User owner = new User();
        ReflectionTestUtils.setField(owner, "id", OWNER_ID);
        Breed breed = new Breed();
        when(breedsService.getBreedById(BREED_ID)).thenReturn(breed);
        when(userService.getUserByUUID(OWNER_ID)).thenReturn(Optional.of(owner));
        when(petRepository.save(any(Pet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PetResponse response = petService.addPet(OWNER_ID, requestWithBirthDate(TODAY));

        assertThat(response.birthDate()).isEqualTo(TODAY);
    }

    @Test
    void futureBirthDateIsRejected() {
        assertThatThrownBy(() -> petService.addPet(OWNER_ID, requestWithBirthDate(TODAY.plusDays(1))))
                .isInstanceOf(InvalidBirthDate.class);

        verify(petRepository, never()).save(any(Pet.class));
    }

    @Test
    void missingBirthDateIsRejected() {
        assertThatThrownBy(() -> petService.addPet(OWNER_ID, requestWithBirthDate(null)))
                .isInstanceOf(InvalidBirthDate.class);
    }

    @Test
    void unknownBreedIsRejected() {
        when(breedsService.getBreedById(BREED_ID)).thenThrow(new BreedNotFound());

        assertThatThrownBy(() -> petService.addPet(OWNER_ID, requestWithBirthDate(LocalDate.of(2026, 8, 1))))
                .isInstanceOf(BreedNotFound.class);

        verify(petRepository, never()).save(any(Pet.class));
    }

    @Test
    void addPetWithoutVaccinationStatusDefaultsToUnknown() {
        User owner = new User();
        ReflectionTestUtils.setField(owner, "id", OWNER_ID);
        when(breedsService.getBreedById(BREED_ID)).thenReturn(new Breed());
        when(userService.getUserByUUID(OWNER_ID)).thenReturn(Optional.of(owner));
        when(petRepository.save(any(Pet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PetResponse response = petService.addPet(OWNER_ID, requestWithBirthDate(TODAY));

        assertThat(response.vaccinationStatus()).isEqualTo(VaccinationStatus.UNKNOWN);
    }

    @Test
    void ownedPetProfileCombinesPetAndBreedData() {
        Pet pet = petBornOn(BIRTH_DATE_AT_TEN_WEEKS, VaccinationStatus.FIRST_DOSE, 1);
        when(petRepository.findByIdAndOwnerId(PET_ID, OWNER_ID)).thenReturn(Optional.of(pet));

        PetProfile profile = petService.getOwnedPetProfile(OWNER_ID, PET_ID);

        assertThat(profile.id()).isEqualTo(PET_ID);
        assertThat(profile.birthDate()).isEqualTo(BIRTH_DATE_AT_TEN_WEEKS);
        assertThat(profile.sizeCategory()).isEqualTo(SizeCategory.LARGE);
        assertThat(profile.createdOn()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(profile.vaccinationStatus()).isEqualTo(VaccinationStatus.FIRST_DOSE);
        assertThat(profile.vaccinationPromptCount()).isEqualTo(1);
    }

    @Test
    void ownedPetProfileOfSomeoneElsesPetIsNotFound() {
        when(petRepository.findByIdAndOwnerId(PET_ID, OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> petService.getOwnedPetProfile(OWNER_ID, PET_ID)).isInstanceOf(PetNotFound.class);
    }

    @Test
    void changedVaccinationStatusIsStoredWithDate() {
        Pet pet = petBornOn(BIRTH_DATE_AT_TEN_WEEKS, VaccinationStatus.NONE, 0);
        givenOwnedPet(pet);

        petService.updateVaccinationStatus(OWNER_ID, PET_ID, VaccinationStatus.FIRST_DOSE, TODAY);

        assertThat(pet.getVaccinationStatus()).isEqualTo(VaccinationStatus.FIRST_DOSE);
        assertThat(pet.getVaccinationStatusChangedOn()).isEqualTo(TODAY);
        assertThat(pet.getVaccinationPromptCount()).isZero();
    }

    @Test
    void unchangedVaccinationStatusKeepsChangeDate() {
        Pet pet = petBornOn(BIRTH_DATE_AT_TEN_WEEKS, VaccinationStatus.FIRST_DOSE, 0);
        pet.setVaccinationStatusChangedOn(TODAY.minusDays(5));
        givenOwnedPet(pet);

        petService.updateVaccinationStatus(OWNER_ID, PET_ID, VaccinationStatus.FIRST_DOSE, TODAY);

        assertThat(pet.getVaccinationStatusChangedOn()).isEqualTo(TODAY.minusDays(5));
    }

    @Test
    void answeringWhilePromptIsDueCountsAsPrompt() {
        Pet pet = petBornOn(BIRTH_DATE_AT_SIXTEEN_WEEKS, VaccinationStatus.UNKNOWN, 0);
        givenOwnedPet(pet);

        petService.updateVaccinationStatus(OWNER_ID, PET_ID, VaccinationStatus.NONE, TODAY);

        assertThat(pet.getVaccinationPromptCount()).isEqualTo(1);
    }

    @Test
    void completingSeriesWhilePromptIsDueDoesNotCountAsPrompt() {
        Pet pet = petBornOn(BIRTH_DATE_AT_SIXTEEN_WEEKS, VaccinationStatus.FIRST_DOSE, 0);
        givenOwnedPet(pet);

        petService.updateVaccinationStatus(OWNER_ID, PET_ID, VaccinationStatus.SERIES_COMPLETE, TODAY);

        assertThat(pet.getVaccinationPromptCount()).isZero();
    }

    @Test
    void vaccinationStatusUpdateRejectsDateOutOfRange() {
        assertThatThrownBy(() -> petService.updateVaccinationStatus(
                OWNER_ID, PET_ID, VaccinationStatus.NONE, TODAY.plusDays(2)))
                .isInstanceOf(InvalidDate.class);
    }

    @Test
    void dismissingDuePromptIncrementsCount() {
        Pet pet = petBornOn(BIRTH_DATE_AT_SIXTEEN_WEEKS, VaccinationStatus.UNKNOWN, 0);
        givenOwnedPet(pet);

        petService.dismissVaccinationPrompt(OWNER_ID, PET_ID, TODAY);

        assertThat(pet.getVaccinationPromptCount()).isEqualTo(1);
        verify(petRepository).save(pet);
    }

    @Test
    void dismissingWhenNoPromptIsDueChangesNothing() {
        Pet pet = petBornOn(BIRTH_DATE_AT_TEN_WEEKS, VaccinationStatus.UNKNOWN, 0);
        givenOwnedPet(pet);

        petService.dismissVaccinationPrompt(OWNER_ID, PET_ID, TODAY);

        assertThat(pet.getVaccinationPromptCount()).isZero();
        verify(petRepository, never()).save(any(Pet.class));
    }

    @Test
    void raisingStatusAboveCurrentUpdatesStatusAndChangedOn() {
        Pet pet = petBornOn(BIRTH_DATE_AT_TEN_WEEKS, VaccinationStatus.UNKNOWN, 0);
        when(petRepository.findById(PET_ID)).thenReturn(Optional.of(pet));

        petService.raiseVaccinationStatus(PET_ID, VaccinationStatus.FIRST_DOSE, TODAY);

        assertThat(pet.getVaccinationStatus()).isEqualTo(VaccinationStatus.FIRST_DOSE);
        assertThat(pet.getVaccinationStatusChangedOn()).isEqualTo(TODAY);
        verify(petRepository).save(pet);
    }

    @Test
    void raisingStatusNeverLowersIt() {
        Pet pet = petBornOn(BIRTH_DATE_AT_TEN_WEEKS, VaccinationStatus.SERIES_COMPLETE, 0);
        when(petRepository.findById(PET_ID)).thenReturn(Optional.of(pet));

        petService.raiseVaccinationStatus(PET_ID, VaccinationStatus.FIRST_DOSE, TODAY);

        assertThat(pet.getVaccinationStatus()).isEqualTo(VaccinationStatus.SERIES_COMPLETE);
        verify(petRepository, never()).save(any(Pet.class));
    }

    @Test
    void raisingToSameStatusChangesNothing() {
        Pet pet = petBornOn(BIRTH_DATE_AT_TEN_WEEKS, VaccinationStatus.FIRST_DOSE, 0);
        when(petRepository.findById(PET_ID)).thenReturn(Optional.of(pet));

        petService.raiseVaccinationStatus(PET_ID, VaccinationStatus.FIRST_DOSE, TODAY);

        assertThat(pet.getVaccinationStatusChangedOn()).isNull();
        verify(petRepository, never()).save(any(Pet.class));
    }

    @Test
    void raisingStatusOfMissingPetFails() {
        when(petRepository.findById(PET_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> petService.raiseVaccinationStatus(PET_ID, VaccinationStatus.FIRST_DOSE, TODAY))
                .isInstanceOf(PetNotFound.class);
    }

    @Test
    void updatePetReplacesEditableFieldsAndReturnsNewBreedSize() {
        Pet pet = petBornOn(BIRTH_DATE_AT_TEN_WEEKS, VaccinationStatus.FIRST_DOSE, 0);
        givenOwnedPet(pet);
        Breed toy = new Breed();
        ReflectionTestUtils.setField(toy, "id", BigInteger.TWO);
        ReflectionTestUtils.setField(toy, "code", "POODLE_TOY");
        ReflectionTestUtils.setField(toy, "sizeCategory", SizeCategory.SMALL);
        when(breedsService.getBreedById(BigInteger.TWO)).thenReturn(toy);

        PetResponse response = petService.updatePet(OWNER_ID, PET_ID, updateRequest(" Mila ", "616 093 900 004 821"));

        assertThat(response.name()).isEqualTo("Mila");
        assertThat(response.sex()).isEqualTo(Sex.FEMALE);
        assertThat(response.birthDate()).isEqualTo(TODAY.minusWeeks(20));
        assertThat(response.neutered()).isTrue();
        assertThat(response.microchipNumber()).isEqualTo("616093900004821");
        assertThat(response.breedCode()).isEqualTo("POODLE_TOY");
        assertThat(response.sizeCategory()).isEqualTo(SizeCategory.SMALL);
    }

    @Test
    void updatePetOfSomeoneElseIsNotFound() {
        when(petRepository.findByIdAndOwnerId(PET_ID, OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> petService.updatePet(OWNER_ID, PET_ID, updateRequest("Mila", null)))
                .isInstanceOf(PetNotFound.class);
    }

    @Test
    void updatePetRejectsFutureBirthDate() {
        UpdatePetRequest request = new UpdatePetRequest("Mila", Sex.FEMALE, TODAY.plusDays(1), BigInteger.TWO, true, null);

        assertThatThrownBy(() -> petService.updatePet(OWNER_ID, PET_ID, request)).isInstanceOf(InvalidBirthDate.class);

        verify(petRepository, never()).save(any(Pet.class));
    }

    @Test
    void updatePetRejectsUnknownBreed() {
        givenOwnedPet(petBornOn(BIRTH_DATE_AT_TEN_WEEKS, VaccinationStatus.NONE, 0));
        when(breedsService.getBreedById(BigInteger.TWO)).thenThrow(new BreedNotFound());

        assertThatThrownBy(() -> petService.updatePet(OWNER_ID, PET_ID, updateRequest("Mila", null)))
                .isInstanceOf(BreedNotFound.class);
    }

    @Test
    void updatePetRejectsInvalidChip() {
        givenOwnedPet(petBornOn(BIRTH_DATE_AT_TEN_WEEKS, VaccinationStatus.NONE, 0));

        assertThatThrownBy(() -> petService.updatePet(OWNER_ID, PET_ID, updateRequest("Mila", "12345")))
                .isInstanceOf(InvalidMicrochip.class);

        verify(petRepository, never()).save(any(Pet.class));
    }

    @Test
    void updatePetWithBlankChipClearsIt() {
        Pet pet = petBornOn(BIRTH_DATE_AT_TEN_WEEKS, VaccinationStatus.NONE, 0);
        pet.setMicrochipNumber("616093900004821");
        givenOwnedPet(pet);
        when(breedsService.getBreedById(BigInteger.TWO)).thenReturn(pet.getBreed());

        petService.updatePet(OWNER_ID, PET_ID, updateRequest("Mila", "   "));

        assertThat(pet.getMicrochipNumber()).isNull();
    }

    @Test
    void addPetRejectsInvalidChip() {
        AddPetRequest request = new AddPetRequest(BREED_ID, "Luna", Sex.FEMALE, TODAY, false, "12345", null);
        when(breedsService.getBreedById(BREED_ID)).thenReturn(new Breed());
        when(userService.getUserByUUID(OWNER_ID)).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> petService.addPet(OWNER_ID, request)).isInstanceOf(InvalidMicrochip.class);

        verify(petRepository, never()).save(any(Pet.class));
    }

    private UpdatePetRequest updateRequest(String name, String chip) {
        return new UpdatePetRequest(name, Sex.FEMALE, TODAY.minusWeeks(20), BigInteger.TWO, true, chip);
    }

    private void givenOwnedPet(Pet pet) {
        when(petRepository.findByIdAndOwnerId(PET_ID, OWNER_ID)).thenReturn(Optional.of(pet));
        lenient().when(petRepository.save(any(Pet.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Pet petBornOn(LocalDate birthDate, VaccinationStatus status, int promptCount) {
        User owner = new User();
        ReflectionTestUtils.setField(owner, "id", OWNER_ID);
        Breed breed = new Breed();
        ReflectionTestUtils.setField(breed, "id", BREED_ID);
        ReflectionTestUtils.setField(breed, "code", "LABRADOR");
        ReflectionTestUtils.setField(breed, "sizeCategory", SizeCategory.LARGE);
        Pet pet = new Pet();
        ReflectionTestUtils.setField(pet, "id", PET_ID);
        ReflectionTestUtils.setField(pet, "createdAt", Instant.parse("2026-10-01T09:30:00Z"));
        pet.setOwner(owner);
        pet.setBreed(breed);
        pet.setName("Luna");
        pet.setBirthDate(birthDate);
        pet.setVaccinationStatus(status);
        pet.setVaccinationPromptCount(promptCount);
        return pet;
    }

    private AddPetRequest requestWithBirthDate(LocalDate birthDate) {
        return new AddPetRequest(BREED_ID, "Luna", Sex.FEMALE, birthDate, false, null, null);
    }
}
