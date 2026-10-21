package com.picvi.pet_companion_service.nutrition.domain;

import com.picvi.pet_companion_service.nutrition.dto.ProteinExposureDto;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Turns the exposure rows into "proteins she has eaten": per ingredient the food kinds, first and last day. */
public class ProteinExposureCalculator {

    public List<ProteinExposureDto> calculate(List<ExposureRow> rows) {
        Map<IngredientCode, List<ExposureRow>> byCode = new TreeMap<>();
        rows.forEach(row -> byCode.computeIfAbsent(row.code(), code -> new java.util.ArrayList<>()).add(row));

        return byCode.entrySet().stream()
                .map(entry -> this.exposure(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing((ProteinExposureDto e) -> e.sourceKind().ordinal())
                        .thenComparing(ProteinExposureDto::firstOn)
                        .thenComparing(e -> e.code().ordinal()))
                .toList();
    }

    private ProteinExposureDto exposure(IngredientCode code, List<ExposureRow> rows) {
        Set<FoodKind> kinds = EnumSet.noneOf(FoodKind.class);
        rows.forEach(row -> kinds.add(row.foodKind()));
        LocalDate firstOn = rows.stream().map(ExposureRow::fromOn).min(Comparator.naturalOrder()).orElseThrow();
        boolean stillGiven = rows.stream().anyMatch(row -> row.toOn() == null);
        LocalDate lastOn = stillGiven ? null
                : rows.stream().map(row -> row.toOn().minusDays(1)).max(Comparator.naturalOrder()).orElseThrow();
        boolean hydrolysed = rows.stream().allMatch(ExposureRow::hydrolysed);
        return new ProteinExposureDto(code, code.getKind(), List.copyOf(kinds), firstOn, lastOn, hydrolysed);
    }
}
