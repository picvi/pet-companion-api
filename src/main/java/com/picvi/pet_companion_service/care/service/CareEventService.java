package com.picvi.pet_companion_service.care.service;

import com.picvi.pet_companion_service.care.domain.VaccinationStatusRule;
import com.picvi.pet_companion_service.care.dto.CareEventDto;
import com.picvi.pet_companion_service.care.dto.CareEventsResponse;
import com.picvi.pet_companion_service.care.dto.CareTimelineRow;
import com.picvi.pet_companion_service.care.entity.CareEvent;
import com.picvi.pet_companion_service.care.entity.CareItem;
import com.picvi.pet_companion_service.care.repository.CareEventRepository;
import com.picvi.pet_companion_service.care.repository.CareItemRepository;
import com.picvi.pet_companion_service.common.ClientDateValidator;
import com.picvi.pet_companion_service.exception.care.CareEventNotFound;
import com.picvi.pet_companion_service.exception.care.CareItemNotFound;
import com.picvi.pet_companion_service.exception.care.InvalidEventDate;
import com.picvi.pet_companion_service.pet.dto.PetProfile;
import com.picvi.pet_companion_service.pet.service.PetService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class CareEventService {
    private final ClientDateValidator clientDateValidator;
    private final PetService petService;
    private final CareItemRepository itemRepository;
    private final CareEventRepository eventRepository;
    private final CareDefinitionLoader definitionLoader;
    private final VaccinationStatusRule vaccinationStatusRule;

    public CareEventDto log(UUID ownerId, UUID petId, String itemCode, LocalDate performedOn, LocalDate date) {
        this.clientDateValidator.validate(date);
        PetProfile pet = this.petService.getOwnedPetProfile(ownerId, petId);
        CareItem item = this.findItem(itemCode);
        this.validatePerformedOn(performedOn, pet.birthDate(), date);

        CareEvent event = this.eventRepository.save(new CareEvent(this.petService.getReference(petId), item, performedOn));
        this.syncVaccinationStatus(petId, item, date);
        return CareEventDto.of(event);
    }

    @Transactional(readOnly = true)
    public List<CareTimelineRow> timelinePage(UUID petId, LocalDate date, boolean includeWholeDay, UUID idBefore, int limit) {
        return this.eventRepository.findTimelinePage(petId, date, includeWholeDay, idBefore, PageRequest.of(0, limit + 1)).stream()
                .map(CareTimelineRow::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public CareEventsResponse history(UUID ownerId, UUID petId, String itemCode) {
        this.petService.getOwnedPetProfile(ownerId, petId);
        this.findItem(itemCode);

        return new CareEventsResponse(this.eventRepository.findAllByPetIdAndItemCodeOrderByPerformedOnDesc(petId, itemCode).stream()
                .map(CareEventDto::of)
                .toList());
    }

    public void undo(UUID ownerId, UUID petId, UUID eventId, LocalDate date) {
        this.clientDateValidator.validate(date);
        this.petService.getOwnedPetProfile(ownerId, petId);

        CareEvent event = this.eventRepository.findByIdAndPetId(eventId, petId).orElseThrow(CareEventNotFound::new);
        this.eventRepository.delete(event);
    }

    private void syncVaccinationStatus(UUID petId, CareItem item, LocalDate date) {
        if (!item.isDrivesVaccinationStatus()) {
            return;
        }
        int doseCount = (int) this.eventRepository.findAllByPetIdAndItemCodeOrderByPerformedOnDesc(petId, item.getCode()).stream()
                .map(CareEvent::getPerformedOn)
                .distinct()
                .count();
        int seriesTotal = this.definitionLoader.load(item).seriesTotal();

        this.vaccinationStatusRule.statusFor(doseCount, seriesTotal)
                .ifPresent(status -> this.petService.raiseVaccinationStatus(petId, status, date));
    }

    private CareItem findItem(String itemCode) {
        return this.itemRepository.findByCodeAndActiveTrue(itemCode).orElseThrow(CareItemNotFound::new);
    }

    private void validatePerformedOn(LocalDate performedOn, LocalDate birthDate, LocalDate today) {
        if (performedOn.isAfter(today) || performedOn.isBefore(birthDate)) {
            throw new InvalidEventDate();
        }
    }
}
