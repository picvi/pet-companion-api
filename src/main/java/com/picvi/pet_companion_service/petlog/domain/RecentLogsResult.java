package com.picvi.pet_companion_service.petlog.domain;

import com.picvi.pet_companion_service.petlog.dto.LastWeightDto;
import com.picvi.pet_companion_service.petlog.dto.MedicineDto;
import com.picvi.pet_companion_service.petlog.dto.RepeatDto;

import java.util.List;
import java.util.UUID;

public record RecentLogsResult(List<RepeatDto> repeats,
                               List<String> foods,
                               List<UUID> petFoodIds,
                               List<MedicineDto> medicines,
                               LastWeightDto lastWeight) {
}
