package com.picvi.pet_companion_service.petlog.dto;

import com.picvi.pet_companion_service.petlog.domain.LogDetail;
import com.picvi.pet_companion_service.petlog.domain.LogType;

public record RepeatDto(LogType type,
                        LogDetail detail,
                        Boolean accident,
                        Integer amountGrams,
                        String label,
                        String dose,
                        Integer durationMinutes) {
}
