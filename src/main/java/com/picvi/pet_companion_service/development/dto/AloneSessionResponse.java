package com.picvi.pet_companion_service.development.dto;

public record AloneSessionResponse(AloneSessionDto session, StepChange change, AloneOverviewResponse overview) {

    public enum StepChange { UP, SAME, DOWN }
}
