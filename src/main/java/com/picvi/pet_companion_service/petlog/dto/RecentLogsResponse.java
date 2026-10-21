package com.picvi.pet_companion_service.petlog.dto;

import java.util.List;
import java.util.UUID;

public record RecentLogsResponse(List<RepeatDto> repeats,
                                 List<String> foods,
                                 List<UUID> petFoodIds,
                                 List<MedicineDto> medicines,
                                 LastWeightDto lastWeight) {
}
