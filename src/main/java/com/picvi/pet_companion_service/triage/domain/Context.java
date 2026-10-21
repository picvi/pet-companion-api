package com.picvi.pet_companion_service.triage.domain;

public record Context(Integer ageWeeks, String sizeCategory, String breedCode, int dhppiDoses, Integer dhppiTotal,
                      VaccineHistory vaccineHistory, Integer daysSinceFoodChange, Integer hoursSinceVaccine,
                      Integer relatedLogs24h, int relatedLogs7d, Integer relatedChecks24h, Integer awakeMinutes) {
}
