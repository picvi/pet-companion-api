package com.picvi.pet_companion_service.triage.dto;

import com.picvi.pet_companion_service.triage.domain.CheckSource;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Map;
import java.util.List;
import java.util.UUID;

public record CreateCheckRequest(@NotNull UUID clientId,
                                 @NotNull LocalDate date,
                                 @NotNull LocalDate occurredOn,
                                 @Min(0) @Max(1439) int minuteOfDay,
                                 @NotNull CheckSource source,
                                 @NotBlank String item,
                                 @NotNull List<String> flags,
                                 @NotNull Map<String, String> answers,
                                 String substance,
                                 @Size(max = 500) String note) {
}
