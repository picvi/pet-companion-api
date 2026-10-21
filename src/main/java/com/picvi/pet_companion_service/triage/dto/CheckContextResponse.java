package com.picvi.pet_companion_service.triage.dto;

import com.picvi.pet_companion_service.breeds.entity.SizeCategory;
import com.picvi.pet_companion_service.triage.domain.VaccineHistory;

import java.time.LocalDate;
import java.util.Map;

public record CheckContextResponse(String item, Integer ageWeeks, SizeCategory sizeCategory, String breedCode,
                                   int dhppiDoses, Integer dhppiTotal, VaccineHistory vaccineHistory,
                                   Integer daysSinceFoodChange, Integer hoursSinceVaccine,
                                   Related related, Map<String, String> prefill) {
    public CheckContextResponse {
        prefill = Map.copyOf(prefill);
    }

    public record Related(Integer count24h, Integer count7d, LocalDate since) {
    }
}
