package com.picvi.pet_companion_service.lostdog.dto;

import com.picvi.pet_companion_service.lostdog.domain.LostStatus;
import com.picvi.pet_companion_service.lostdog.domain.LostStepCode;
import com.picvi.pet_companion_service.lostdog.entity.LostReport;
import com.picvi.pet_companion_service.lostdog.entity.LostReportStep;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public record LostReportDto(UUID id, UUID petId, Instant startedAt, Instant lastSeenAt, String lastSeenPlace,
                            Double lastSeenLat, Double lastSeenLng, String posterPhone, String posterMessage, LostStatus status,
                            Instant endedAt, long durationSeconds, long version, List<LostReportStepDto> steps) {
    public static LostReportDto of(LostReport report, List<LostReportStep> storedSteps) {
        Map<LostStepCode, Instant> completed = storedSteps.stream()
                .filter(step -> step.getDoneAt() != null)
                .collect(Collectors.toMap(step -> step.getId().getStepCode(), LostReportStep::getDoneAt));
        List<LostReportStepDto> steps = Arrays.stream(com.picvi.pet_companion_service.lostdog.domain.LostStepCode.values())
                .map(code -> new LostReportStepDto(code, completed.get(code))).toList();
        Instant end = report.getEndedAt() == null ? Instant.now() : report.getEndedAt();
        long duration = Math.max(0, Duration.between(report.getStartedAt(), end).getSeconds());
        return new LostReportDto(report.getId(), report.getPetId(), report.getStartedAt(), report.getLastSeenAt(),
                report.getLastSeenPlace(), report.getLastSeenLat(), report.getLastSeenLng(), report.getPosterPhone(), report.getPosterMessage(),
                report.getStatus(), report.getEndedAt(), duration, report.getVersion(), steps);
    }
}
