package com.picvi.pet_companion_service.care.service;

import com.picvi.pet_companion_service.care.domain.CareItemDefinition;
import com.picvi.pet_companion_service.care.domain.CareItemSchedule;
import com.picvi.pet_companion_service.care.domain.CareItemSettings;
import com.picvi.pet_companion_service.care.domain.CareScheduleCalculator;
import com.picvi.pet_companion_service.care.dto.CareItemDto;
import com.picvi.pet_companion_service.care.dto.CareScheduleResponse;
import com.picvi.pet_companion_service.care.api.CoreVaccineProgress;
import com.picvi.pet_companion_service.care.entity.CareEvent;
import com.picvi.pet_companion_service.care.entity.PetCareSetting;
import com.picvi.pet_companion_service.care.repository.CareEventRepository;
import com.picvi.pet_companion_service.care.repository.PetCareSettingRepository;
import com.picvi.pet_companion_service.common.ClientDateValidator;
import com.picvi.pet_companion_service.exception.care.CareItemNotFound;
import com.picvi.pet_companion_service.pet.dto.PetProfile;
import com.picvi.pet_companion_service.pet.service.PetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CareScheduleService {
    private final ClientDateValidator clientDateValidator;
    private final PetService petService;
    private final CareDefinitionLoader definitionLoader;
    private final CareEventRepository eventRepository;
    private final PetCareSettingRepository settingRepository;
    private final CareScheduleCalculator calculator;

    public CareScheduleResponse getSchedule(UUID ownerId, UUID petId, LocalDate date) {
        this.clientDateValidator.validate(date);
        PetProfile pet = this.petService.getOwnedPetProfile(ownerId, petId);

        Map<String, List<LocalDate>> eventDates = this.eventDatesByItem(petId);
        Map<String, CareItemSettings> settings = this.settingsByItem(petId);
        List<CareItemSchedule> schedules = this.definitionLoader.loadAll().stream()
                .map(item -> this.scheduleFor(item, pet, date, eventDates, settings))
                .toList();

        return new CareScheduleResponse(
                date,
                (int) (ChronoUnit.DAYS.between(pet.birthDate(), date) / 7),
                this.calculator.alertCode(schedules).orElse(null),
                this.calculator.order(schedules).stream().map(CareItemDto::of).toList()
        );
    }

    public CoreVaccineProgress coreVaccineDoses(UUID ownerId, UUID petId, LocalDate date) {
        this.clientDateValidator.validate(date);
        this.petService.getOwnedPetProfile(ownerId, petId);
        int total = this.definitionLoader.loadAll().stream()
                .filter(item -> item.code().equals("DHPPI"))
                .findFirst()
                .map(CareItemDefinition::seriesTotal)
                .orElse(0);
        long completed = this.eventRepository.findAllByPetIdAndItemCodeOrderByPerformedOnDesc(petId, "DHPPI").stream()
                .filter(event -> !event.getPerformedOn().isAfter(date))
                .count();
        return new CoreVaccineProgress((int) Math.min(completed, total), total);
    }

    public CareItemDto getItem(UUID petId, PetProfile pet, String code, LocalDate date) {
        CareItemDefinition item = this.definitionLoader.loadAll().stream()
                .filter(definition -> definition.code().equals(code))
                .findFirst()
                .orElseThrow(CareItemNotFound::new);

        return CareItemDto.of(this.scheduleFor(item, pet, date, this.eventDatesByItem(petId), this.settingsByItem(petId)));
    }

    private CareItemSchedule scheduleFor(CareItemDefinition item,
                                         PetProfile pet,
                                         LocalDate date,
                                         Map<String, List<LocalDate>> eventDates,
                                         Map<String, CareItemSettings> settings) {
        return this.calculator.calculate(
                item,
                pet.birthDate(),
                eventDates.getOrDefault(item.code(), List.of()),
                settings.getOrDefault(item.code(), CareItemSettings.defaultsFor(item.requirement())),
                date
        );
    }

    private Map<String, List<LocalDate>> eventDatesByItem(UUID petId) {
        return this.eventRepository.findAllByPetId(petId).stream()
                .collect(Collectors.groupingBy(
                        event -> event.getItem().getCode(),
                        Collectors.mapping(CareEvent::getPerformedOn, Collectors.toList())));
    }

    private Map<String, CareItemSettings> settingsByItem(UUID petId) {
        return this.settingRepository.findAllByPetId(petId).stream()
                .collect(Collectors.toMap(setting -> setting.getItem().getCode(), this::toSettings));
    }

    private CareItemSettings toSettings(PetCareSetting setting) {
        return new CareItemSettings(setting.isRemindersEnabled(), setting.getPausedUntil(), setting.getIntervalDaysOverride());
    }
}
