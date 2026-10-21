package com.picvi.pet_companion_service.care.service;

import com.picvi.pet_companion_service.breeds.entity.SizeCategory;
import com.picvi.pet_companion_service.care.CareCatalogueFixture;
import com.picvi.pet_companion_service.care.CareEntities;
import com.picvi.pet_companion_service.care.domain.CareScheduleCalculator;
import com.picvi.pet_companion_service.care.domain.CareStatus;
import com.picvi.pet_companion_service.care.dto.CareItemDto;
import com.picvi.pet_companion_service.care.dto.CareScheduleResponse;
import com.picvi.pet_companion_service.care.entity.CareItem;
import com.picvi.pet_companion_service.care.entity.PetCareSetting;
import com.picvi.pet_companion_service.care.repository.CareEventRepository;
import com.picvi.pet_companion_service.care.repository.PetCareSettingRepository;
import com.picvi.pet_companion_service.common.ClientDateValidator;
import com.picvi.pet_companion_service.exception.InvalidDate;
import com.picvi.pet_companion_service.exception.care.CareItemNotFound;
import com.picvi.pet_companion_service.pet.dto.PetProfile;
import com.picvi.pet_companion_service.pet.entity.VaccinationStatus;
import com.picvi.pet_companion_service.pet.service.PetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CareScheduleServiceTest {
    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final UUID PET_ID = UUID.randomUUID();
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 14);
    private static final LocalDate BIRTH_DATE = TODAY.minusWeeks(10);

    @Mock
    private ClientDateValidator clientDateValidator;
    @Mock
    private PetService petService;
    @Mock
    private CareDefinitionLoader definitionLoader;
    @Mock
    private CareEventRepository eventRepository;
    @Mock
    private PetCareSettingRepository settingRepository;

    private CareScheduleService service;
    private PetProfile pet;

    @BeforeEach
    void setUp() {
        this.service = new CareScheduleService(this.clientDateValidator, this.petService, this.definitionLoader,
                this.eventRepository, this.settingRepository, new CareScheduleCalculator());
        this.pet = new PetProfile(PET_ID, "Rex", BIRTH_DATE, SizeCategory.MEDIUM, BIRTH_DATE, VaccinationStatus.UNKNOWN, null, 0);
    }

    private void givenOwnedPet() {
        when(this.petService.getOwnedPetProfile(OWNER_ID, PET_ID)).thenReturn(this.pet);
    }

    private void givenCatalogue() {
        when(this.definitionLoader.loadAll()).thenReturn(List.of(
                CareCatalogueFixture.RABIES, CareCatalogueFixture.DHPPI, CareCatalogueFixture.DEWORMING));
    }

    @Test
    void returnsAgeInWeeksAndAllItems() {
        givenOwnedPet();
        givenCatalogue();
        CareScheduleResponse response = this.service.getSchedule(OWNER_ID, PET_ID, TODAY);

        assertThat(response.ageWeeks()).isEqualTo(10);
        assertThat(response.date()).isEqualTo(TODAY);
        assertThat(response.items()).extracting(CareItemDto::code)
                .containsExactlyInAnyOrder("RABIES", "DHPPI", "DEWORMING");
    }

    @Test
    void usesLoggedEventsOfTheRightItem() {
        givenOwnedPet();
        givenCatalogue();
        CareItem dhppi = CareEntities.item(CareCatalogueFixture.DHPPI);
        when(this.eventRepository.findAllByPetId(PET_ID)).thenReturn(List.of(
                CareEntities.event(dhppi, TODAY.minusWeeks(2))));

        CareScheduleResponse response = this.service.getSchedule(OWNER_ID, PET_ID, TODAY);

        CareItemDto dhppiDto = response.items().stream().filter(item -> item.code().equals("DHPPI")).findFirst().orElseThrow();
        CareItemDto rabiesDto = response.items().stream().filter(item -> item.code().equals("RABIES")).findFirst().orElseThrow();
        assertThat(dhppiDto.lastPerformedOn()).isEqualTo(TODAY.minusWeeks(2));
        assertThat(rabiesDto.lastPerformedOn()).isNull();
    }

    @Test
    void appliesStoredCustomInterval() {
        givenOwnedPet();
        givenCatalogue();
        CareItem deworming = CareEntities.item(CareCatalogueFixture.DEWORMING);
        PetCareSetting setting = new PetCareSetting(null, deworming);
        setting.setRemindersEnabled(true);
        setting.setIntervalDaysOverride(60);
        when(this.settingRepository.findAllByPetId(PET_ID)).thenReturn(List.of(setting));

        CareScheduleResponse response = this.service.getSchedule(OWNER_ID, PET_ID, TODAY);

        CareItemDto dewormingDto = response.items().stream().filter(item -> item.code().equals("DEWORMING")).findFirst().orElseThrow();
        assertThat(dewormingDto.customIntervalDays()).isEqualTo(60);
    }

    @Test
    void puppyWithNoDosesHasDueItemsAndAlert() {
        givenOwnedPet();
        givenCatalogue();
        CareScheduleResponse response = this.service.getSchedule(OWNER_ID, PET_ID, TODAY);

        assertThat(response.items()).anyMatch(item -> item.status() == CareStatus.DUE || item.status() == CareStatus.OVERDUE);
        assertThat(response.alertCode()).isNotNull();
    }

    @Test
    void returnsSingleItem() {
        givenCatalogue();
        CareItemDto dto = this.service.getItem(PET_ID, this.pet, "DHPPI", TODAY);

        assertThat(dto.code()).isEqualTo("DHPPI");
    }

    @Test
    void rejectsUnknownItem() {
        givenCatalogue();
        assertThatThrownBy(() -> this.service.getItem(PET_ID, this.pet, "NOPE", TODAY))
                .isInstanceOf(CareItemNotFound.class);
    }

    @Test
    void rejectsInvalidClientDate() {
        doThrow(new InvalidDate()).when(this.clientDateValidator).validate(TODAY);

        assertThatThrownBy(() -> this.service.getSchedule(OWNER_ID, PET_ID, TODAY))
                .isInstanceOf(InvalidDate.class);
    }
}
