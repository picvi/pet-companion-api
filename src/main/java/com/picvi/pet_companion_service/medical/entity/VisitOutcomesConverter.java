package com.picvi.pet_companion_service.medical.entity;

import com.picvi.pet_companion_service.medical.domain.VisitOutcome;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

@Converter
public class VisitOutcomesConverter implements AttributeConverter<Set<VisitOutcome>, String> {

    @Override
    public String convertToDatabaseColumn(Set<VisitOutcome> outcomes) {
        if (outcomes == null) {
            return "";
        }
        return outcomes.stream().sorted(Comparator.naturalOrder()).map(Enum::name).collect(Collectors.joining(","));
    }

    @Override
    public Set<VisitOutcome> convertToEntityAttribute(String column) {
        Set<VisitOutcome> outcomes = EnumSet.noneOf(VisitOutcome.class);
        if (column != null && !column.isBlank()) {
            Arrays.stream(column.split(",")).map(VisitOutcome::valueOf).forEach(outcomes::add);
        }
        return outcomes;
    }
}
