package com.picvi.pet_companion_service.triage.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Converter
public class CheckFlagsConverter implements AttributeConverter<Set<String>, String> {

    @Override
    public String convertToDatabaseColumn(Set<String> flags) {
        return flags == null ? "" : new TreeSet<>(flags).stream().collect(Collectors.joining(","));
    }

    @Override
    public Set<String> convertToEntityAttribute(String column) {
        return column == null || column.isBlank() ? Set.of() : Arrays.stream(column.split(",")).collect(Collectors.toUnmodifiableSet());
    }
}
