package com.picvi.pet_companion_service.triage.dto;

import com.picvi.pet_companion_service.triage.domain.CheckSource;
import com.picvi.pet_companion_service.triage.domain.CheckStatus;
import com.picvi.pet_companion_service.triage.domain.Level;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Collections;
import java.util.LinkedHashMap;

/** Immutable saved check and exact result snapshot. */
public record CheckResponse(UUID id, UUID clientId, UUID petId, LocalDate occurredOn, int minuteOfDay, CheckSource source,
                            String item, String category, Set<String> flags, Map<String, String> answers,
                            String substance, String note, Map<String, Object> context, Level level,
                            String ruleId, String rulebook, String reviewStatus, Map<String, Object> result, CheckStatus status,
                            Instant checkBackDueAt, LocalDate resolvedOn, Instant createdAt) {
    public CheckResponse {
        flags = Set.copyOf(flags);
        answers = Collections.unmodifiableMap(new LinkedHashMap<>(answers));
        context = Collections.unmodifiableMap(new LinkedHashMap<>(context));
        result = Collections.unmodifiableMap(new LinkedHashMap<>(result));
    }
}
