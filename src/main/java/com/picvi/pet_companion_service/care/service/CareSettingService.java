package com.picvi.pet_companion_service.care.service;

import com.picvi.pet_companion_service.care.dto.CareItemDto;
import com.picvi.pet_companion_service.care.entity.CareItem;
import com.picvi.pet_companion_service.care.entity.PetCareSetting;
import com.picvi.pet_companion_service.care.repository.CareItemRepository;
import com.picvi.pet_companion_service.care.repository.PetCareSettingRepository;
import com.picvi.pet_companion_service.common.ClientDateValidator;
import com.picvi.pet_companion_service.exception.care.CareItemNotFound;
import com.picvi.pet_companion_service.exception.care.IntervalNotEditable;
import com.picvi.pet_companion_service.exception.care.InvalidInterval;
import com.picvi.pet_companion_service.pet.dto.PetProfile;
import com.picvi.pet_companion_service.pet.service.PetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class CareSettingService {
    private static final int MIN_INTERVAL_DAYS = 7;
    private static final int MAX_INTERVAL_DAYS = 365;

    private final ClientDateValidator clientDateValidator;
    private final PetService petService;
    private final CareItemRepository itemRepository;
    private final PetCareSettingRepository settingRepository;
    private final CareScheduleService scheduleService;

    public CareItemDto update(UUID ownerId,
                              UUID petId,
                              String itemCode,
                              boolean remindersEnabled,
                              LocalDate pausedUntil,
                              Integer intervalDays,
                              LocalDate date) {
        this.clientDateValidator.validate(date);
        PetProfile pet = this.petService.getOwnedPetProfile(ownerId, petId);
        CareItem item = this.itemRepository.findByCodeAndActiveTrue(itemCode).orElseThrow(CareItemNotFound::new);
        this.validateInterval(item, intervalDays);

        PetCareSetting setting = this.settingRepository.findByPetIdAndItemCode(petId, itemCode)
                .orElseGet(() -> new PetCareSetting(this.petService.getReference(petId), item));
        setting.setRemindersEnabled(remindersEnabled);
        setting.setPausedUntil(pausedUntil);
        setting.setIntervalDaysOverride(intervalDays);
        this.settingRepository.save(setting);

        return this.scheduleService.getItem(petId, pet, itemCode, date);
    }

    private void validateInterval(CareItem item, Integer intervalDays) {
        if (intervalDays == null) {
            return;
        }
        if (!item.isIntervalEditable()) {
            throw new IntervalNotEditable();
        }
        if (intervalDays < MIN_INTERVAL_DAYS || intervalDays > MAX_INTERVAL_DAYS) {
            throw new InvalidInterval();
        }
    }
}
