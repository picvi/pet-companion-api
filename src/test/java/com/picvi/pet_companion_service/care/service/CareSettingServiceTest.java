package com.picvi.pet_companion_service.care.service;

import com.picvi.pet_companion_service.breeds.entity.SizeCategory;
import com.picvi.pet_companion_service.care.CareCatalogueFixture;
import com.picvi.pet_companion_service.care.CareEntities;
import com.picvi.pet_companion_service.care.domain.CareItemDefinition;
import com.picvi.pet_companion_service.care.entity.CareItem;
import com.picvi.pet_companion_service.care.entity.PetCareSetting;
import com.picvi.pet_companion_service.care.repository.CareItemRepository;
import com.picvi.pet_companion_service.care.repository.PetCareSettingRepository;
import com.picvi.pet_companion_service.common.ClientDateValidator;
import com.picvi.pet_companion_service.exception.care.CareItemNotFound;
import com.picvi.pet_companion_service.exception.care.IntervalNotEditable;
import com.picvi.pet_companion_service.exception.care.InvalidInterval;
import com.picvi.pet_companion_service.pet.dto.PetProfile;
import com.picvi.pet_companion_service.pet.entity.VaccinationStatus;
import com.picvi.pet_companion_service.pet.service.PetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CareSettingServiceTest {
    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final UUID PET_ID = UUID.randomUUID();
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 14);

    @Mock
    private ClientDateValidator clientDateValidator;
    @Mock
    private PetService petService;
    @Mock
    private CareItemRepository itemRepository;
    @Mock
    private PetCareSettingRepository settingRepository;
    @Mock
    private CareScheduleService scheduleService;

    private CareSettingService service;
    private PetProfile pet;

    @BeforeEach
    void setUp() {
        this.service = new CareSettingService(this.clientDateValidator, this.petService, this.itemRepository, this.settingRepository, this.scheduleService);
        this.pet = new PetProfile(PET_ID, "Rex", TODAY.minusMonths(6), SizeCategory.MEDIUM, TODAY, VaccinationStatus.UNKNOWN, null, 0);
        when(this.petService.getOwnedPetProfile(OWNER_ID, PET_ID)).thenReturn(this.pet);
    }

    private CareItem givenItem(CareItemDefinition definition) {
        CareItem item = CareEntities.item(definition);
        when(this.itemRepository.findByCodeAndActiveTrue(definition.code())).thenReturn(Optional.of(item));
        return item;
    }

    @Test
    void createsSettingWhenNoneExists() {
        givenItem(CareCatalogueFixture.DEWORMING);
        when(this.settingRepository.findByPetIdAndItemCode(PET_ID, "DEWORMING")).thenReturn(Optional.empty());

        this.service.update(OWNER_ID, PET_ID, "DEWORMING", false, null, 60, TODAY);

        ArgumentCaptor<PetCareSetting> saved = ArgumentCaptor.forClass(PetCareSetting.class);
        verify(this.settingRepository).save(saved.capture());
        assertThat(saved.getValue().isRemindersEnabled()).isFalse();
        assertThat(saved.getValue().getIntervalDaysOverride()).isEqualTo(60);
    }

    @Test
    void updatesExistingSetting() {
        CareItem item = givenItem(CareCatalogueFixture.DEWORMING);
        PetCareSetting existing = new PetCareSetting(null, item);
        existing.setIntervalDaysOverride(60);
        when(this.settingRepository.findByPetIdAndItemCode(PET_ID, "DEWORMING")).thenReturn(Optional.of(existing));

        this.service.update(OWNER_ID, PET_ID, "DEWORMING", true, TODAY.plusDays(10), null, TODAY);

        verify(this.settingRepository).save(existing);
        assertThat(existing.isRemindersEnabled()).isTrue();
        assertThat(existing.getPausedUntil()).isEqualTo(TODAY.plusDays(10));
        assertThat(existing.getIntervalDaysOverride()).isNull();
    }

    @Test
    void acceptsBoundaryIntervals() {
        givenItem(CareCatalogueFixture.TICK_PROTECTION);
        when(this.settingRepository.findByPetIdAndItemCode(PET_ID, "TICK_PROTECTION")).thenReturn(Optional.empty());

        this.service.update(OWNER_ID, PET_ID, "TICK_PROTECTION", true, null, 7, TODAY);
        this.service.update(OWNER_ID, PET_ID, "TICK_PROTECTION", true, null, 365, TODAY);

        verify(this.settingRepository, times(2)).save(any(PetCareSetting.class));
    }

    @Test
    void rejectsIntervalBelowMinimum() {
        givenItem(CareCatalogueFixture.DEWORMING);

        assertThatThrownBy(() -> this.service.update(OWNER_ID, PET_ID, "DEWORMING", true, null, 6, TODAY))
                .isInstanceOf(InvalidInterval.class);
        verify(this.settingRepository, never()).save(any());
    }

    @Test
    void rejectsIntervalAboveMaximum() {
        givenItem(CareCatalogueFixture.DEWORMING);

        assertThatThrownBy(() -> this.service.update(OWNER_ID, PET_ID, "DEWORMING", true, null, 366, TODAY))
                .isInstanceOf(InvalidInterval.class);
    }

    @Test
    void rejectsIntervalForNonEditableItem() {
        givenItem(CareCatalogueFixture.RABIES);

        assertThatThrownBy(() -> this.service.update(OWNER_ID, PET_ID, "RABIES", true, null, 60, TODAY))
                .isInstanceOf(IntervalNotEditable.class);
        verify(this.settingRepository, never()).save(any());
    }

    @Test
    void allowsRemindersToggleOnNonEditableItem() {
        givenItem(CareCatalogueFixture.RABIES);
        when(this.settingRepository.findByPetIdAndItemCode(PET_ID, "RABIES")).thenReturn(Optional.empty());

        this.service.update(OWNER_ID, PET_ID, "RABIES", false, null, null, TODAY);

        verify(this.settingRepository).save(any(PetCareSetting.class));
    }

    @Test
    void rejectsUnknownItem() {
        when(this.itemRepository.findByCodeAndActiveTrue("NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> this.service.update(OWNER_ID, PET_ID, "NOPE", true, null, null, TODAY))
                .isInstanceOf(CareItemNotFound.class);
    }
}
