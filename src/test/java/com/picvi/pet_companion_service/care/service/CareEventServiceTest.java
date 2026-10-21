package com.picvi.pet_companion_service.care.service;

import com.picvi.pet_companion_service.breeds.entity.SizeCategory;
import com.picvi.pet_companion_service.care.CareCatalogueFixture;
import com.picvi.pet_companion_service.care.CareEntities;
import com.picvi.pet_companion_service.care.domain.VaccinationStatusRule;
import com.picvi.pet_companion_service.care.dto.CareEventDto;
import com.picvi.pet_companion_service.care.dto.CareEventsResponse;
import com.picvi.pet_companion_service.care.entity.CareEvent;
import com.picvi.pet_companion_service.care.entity.CareItem;
import com.picvi.pet_companion_service.care.repository.CareEventRepository;
import com.picvi.pet_companion_service.care.repository.CareItemRepository;
import com.picvi.pet_companion_service.common.ClientDateValidator;
import com.picvi.pet_companion_service.exception.InvalidDate;
import com.picvi.pet_companion_service.exception.care.CareEventNotFound;
import com.picvi.pet_companion_service.exception.care.CareItemNotFound;
import com.picvi.pet_companion_service.exception.care.InvalidEventDate;
import com.picvi.pet_companion_service.exception.pet.PetNotFound;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CareEventServiceTest {
    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final UUID PET_ID = UUID.randomUUID();
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 14);
    private static final LocalDate BIRTH_DATE = TODAY.minusMonths(6);
    private static final String CODE = "RABIES";

    @Mock
    private ClientDateValidator clientDateValidator;
    @Mock
    private PetService petService;
    @Mock
    private CareItemRepository itemRepository;
    @Mock
    private CareEventRepository eventRepository;
    @Mock
    private CareDefinitionLoader definitionLoader;

    private CareEventService service;
    private CareItem item;

    @BeforeEach
    void setUp() {
        this.service = new CareEventService(this.clientDateValidator, this.petService, this.itemRepository, this.eventRepository,
                this.definitionLoader, new VaccinationStatusRule());
        this.item = CareEntities.item(CareCatalogueFixture.RABIES);
    }

    private PetProfile pet() {
        return new PetProfile(PET_ID, "Rex", BIRTH_DATE, SizeCategory.MEDIUM, BIRTH_DATE, VaccinationStatus.UNKNOWN, null, 0);
    }

    private void givenOwnedPetAndItem() {
        when(this.petService.getOwnedPetProfile(OWNER_ID, PET_ID)).thenReturn(pet());
        when(this.itemRepository.findByCodeAndActiveTrue(CODE)).thenReturn(Optional.of(this.item));
    }

    @Test
    void logsEventForOwnedPet() {
        givenOwnedPetAndItem();
        when(this.eventRepository.save(any(CareEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        this.service.log(OWNER_ID, PET_ID, CODE, TODAY.minusDays(3), TODAY);

        ArgumentCaptor<CareEvent> saved = ArgumentCaptor.forClass(CareEvent.class);
        verify(this.eventRepository).save(saved.capture());
        assertThat(saved.getValue().getItem()).isSameAs(this.item);
        assertThat(saved.getValue().getPerformedOn()).isEqualTo(TODAY.minusDays(3));
    }

    @Test
    void allowsEventOnToday() {
        givenOwnedPetAndItem();
        when(this.eventRepository.save(any(CareEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CareEventDto dto = this.service.log(OWNER_ID, PET_ID, CODE, TODAY, TODAY);

        assertThat(dto.performedOn()).isEqualTo(TODAY);
    }

    @Test
    void allowsEventOnBirthDate() {
        givenOwnedPetAndItem();
        when(this.eventRepository.save(any(CareEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CareEventDto dto = this.service.log(OWNER_ID, PET_ID, CODE, BIRTH_DATE, TODAY);

        assertThat(dto.performedOn()).isEqualTo(BIRTH_DATE);
    }

    @Test
    void rejectsEventInTheFuture() {
        givenOwnedPetAndItem();

        assertThatThrownBy(() -> this.service.log(OWNER_ID, PET_ID, CODE, TODAY.plusDays(1), TODAY))
                .isInstanceOf(InvalidEventDate.class);
        verify(this.eventRepository, never()).save(any());
    }

    @Test
    void rejectsEventBeforeBirth() {
        givenOwnedPetAndItem();

        assertThatThrownBy(() -> this.service.log(OWNER_ID, PET_ID, CODE, BIRTH_DATE.minusDays(1), TODAY))
                .isInstanceOf(InvalidEventDate.class);
        verify(this.eventRepository, never()).save(any());
    }

    @Test
    void rejectsUnknownItem() {
        when(this.petService.getOwnedPetProfile(OWNER_ID, PET_ID)).thenReturn(pet());
        when(this.itemRepository.findByCodeAndActiveTrue("NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> this.service.log(OWNER_ID, PET_ID, "NOPE", TODAY, TODAY))
                .isInstanceOf(CareItemNotFound.class);
    }

    @Test
    void rejectsPetOfAnotherOwner() {
        when(this.petService.getOwnedPetProfile(OWNER_ID, PET_ID)).thenThrow(new PetNotFound());

        assertThatThrownBy(() -> this.service.log(OWNER_ID, PET_ID, CODE, TODAY, TODAY))
                .isInstanceOf(PetNotFound.class);
        verify(this.eventRepository, never()).save(any());
    }

    @Test
    void rejectsInvalidClientDate() {
        doThrow(new InvalidDate()).when(this.clientDateValidator).validate(TODAY);

        assertThatThrownBy(() -> this.service.log(OWNER_ID, PET_ID, CODE, TODAY, TODAY))
                .isInstanceOf(InvalidDate.class);
        verify(this.eventRepository, never()).save(any());
    }

    @Test
    void returnsHistoryNewestFirst() {
        givenOwnedPetAndItem();
        CareEvent newer = CareEntities.event(this.item, TODAY.minusDays(1));
        CareEvent older = CareEntities.event(this.item, TODAY.minusDays(40));
        when(this.eventRepository.findAllByPetIdAndItemCodeOrderByPerformedOnDesc(PET_ID, CODE)).thenReturn(List.of(newer, older));

        CareEventsResponse response = this.service.history(OWNER_ID, PET_ID, CODE);

        assertThat(response.events()).extracting(CareEventDto::performedOn)
                .containsExactly(TODAY.minusDays(1), TODAY.minusDays(40));
    }

    @Test
    void undoDeletesEvent() {
        CareEvent event = CareEntities.event(this.item, TODAY);
        when(this.petService.getOwnedPetProfile(OWNER_ID, PET_ID)).thenReturn(pet());
        when(this.eventRepository.findByIdAndPetId(event.getId(), PET_ID)).thenReturn(Optional.of(event));

        this.service.undo(OWNER_ID, PET_ID, event.getId(), TODAY);

        verify(this.eventRepository).delete(event);
    }

    @Test
    void undoRejectsEventOfAnotherPet() {
        UUID eventId = UUID.randomUUID();
        when(this.petService.getOwnedPetProfile(OWNER_ID, PET_ID)).thenReturn(pet());
        when(this.eventRepository.findByIdAndPetId(eventId, PET_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> this.service.undo(OWNER_ID, PET_ID, eventId, TODAY))
                .isInstanceOf(CareEventNotFound.class);
        verify(this.eventRepository, never()).delete(any());
    }

    private void givenDhppiWithLoggedDates(LocalDate... dates) {
        CareItem dhppi = CareEntities.item(CareCatalogueFixture.DHPPI, true);
        when(this.petService.getOwnedPetProfile(OWNER_ID, PET_ID)).thenReturn(pet());
        when(this.itemRepository.findByCodeAndActiveTrue("DHPPI")).thenReturn(Optional.of(dhppi));
        when(this.eventRepository.save(any(CareEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(this.definitionLoader.load(dhppi)).thenReturn(CareCatalogueFixture.DHPPI);
        when(this.eventRepository.findAllByPetIdAndItemCodeOrderByPerformedOnDesc(PET_ID, "DHPPI"))
                .thenReturn(Arrays.stream(dates).map(date -> CareEntities.event(dhppi, date)).toList());
    }

    @Test
    void firstDhppiDoseRaisesStatusToFirstDose() {
        givenDhppiWithLoggedDates(TODAY);

        this.service.log(OWNER_ID, PET_ID, "DHPPI", TODAY, TODAY);

        verify(this.petService).raiseVaccinationStatus(PET_ID, VaccinationStatus.FIRST_DOSE, TODAY);
    }

    @Test
    void secondDhppiDoseKeepsFirstDose() {
        givenDhppiWithLoggedDates(TODAY, TODAY.minusDays(21));

        this.service.log(OWNER_ID, PET_ID, "DHPPI", TODAY, TODAY);

        verify(this.petService).raiseVaccinationStatus(PET_ID, VaccinationStatus.FIRST_DOSE, TODAY);
    }

    @Test
    void lastSeriesDoseRaisesStatusToSeriesComplete() {
        givenDhppiWithLoggedDates(TODAY, TODAY.minusDays(21), TODAY.minusDays(42));

        this.service.log(OWNER_ID, PET_ID, "DHPPI", TODAY, TODAY);

        verify(this.petService).raiseVaccinationStatus(PET_ID, VaccinationStatus.SERIES_COMPLETE, TODAY);
    }

    @Test
    void sameDayDuplicatesCountAsOneDose() {
        givenDhppiWithLoggedDates(TODAY, TODAY, TODAY);

        this.service.log(OWNER_ID, PET_ID, "DHPPI", TODAY, TODAY);

        verify(this.petService).raiseVaccinationStatus(PET_ID, VaccinationStatus.FIRST_DOSE, TODAY);
    }

    @Test
    void itemsThatDoNotDriveStatusLeaveItUntouched() {
        givenOwnedPetAndItem();
        when(this.eventRepository.save(any(CareEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        this.service.log(OWNER_ID, PET_ID, CODE, TODAY, TODAY);

        verify(this.petService, never()).raiseVaccinationStatus(any(), any(), any());
    }

    @Test
    void undoDoesNotLowerVaccinationStatus() {
        CareEvent event = CareEntities.event(this.item, TODAY);
        when(this.petService.getOwnedPetProfile(OWNER_ID, PET_ID)).thenReturn(pet());
        when(this.eventRepository.findByIdAndPetId(event.getId(), PET_ID)).thenReturn(Optional.of(event));

        this.service.undo(OWNER_ID, PET_ID, event.getId(), TODAY);

        verify(this.petService, never()).raiseVaccinationStatus(any(), any(), any());
    }
}
