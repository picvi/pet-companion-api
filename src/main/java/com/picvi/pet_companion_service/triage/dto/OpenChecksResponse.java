package com.picvi.pet_companion_service.triage.dto;

import java.time.LocalDate;
import java.util.List;

public record OpenChecksResponse(LocalDate date, List<OpenCheck> checks) {
    public OpenChecksResponse {
        checks = List.copyOf(checks);
    }

    public record OpenCheck(CheckResponse check, String state) {
    }
}
