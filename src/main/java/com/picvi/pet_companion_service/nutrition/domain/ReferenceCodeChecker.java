package com.picvi.pet_companion_service.nutrition.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Compares the IngredientCode enum with the ingredient_source rows. Returns the problems found; empty means they agree. */
public class ReferenceCodeChecker {

    public record Row(String code, IngredientKind kind, String parentCode) {
    }

    public List<String> check(List<Row> rows) {
        List<String> problems = new ArrayList<>();
        Map<String, Row> byCode = new HashMap<>();
        rows.forEach(row -> byCode.put(row.code(), row));

        for (IngredientCode code : IngredientCode.values()) {
            Row row = byCode.get(code.name());
            if (row == null) {
                problems.add(code + " is in the enum but not in ingredient_source");
                continue;
            }
            if (row.kind() != code.getKind()) {
                problems.add(code + " is " + code.getKind() + " in the enum but " + row.kind() + " in ingredient_source");
            }
            String enumParent = code.parent() == null ? null : code.parent().name();
            if (!java.util.Objects.equals(enumParent, row.parentCode())) {
                problems.add(code + " has parent " + enumParent + " in the enum but " + row.parentCode() + " in ingredient_source");
            }
        }

        Set<String> enumNames = new HashSet<>();
        Arrays.stream(IngredientCode.values()).forEach(code -> enumNames.add(code.name()));
        for (Row row : rows) {
            if (!enumNames.contains(row.code())) {
                problems.add(row.code() + " is in ingredient_source but not in the enum");
            }
            if (row.parentCode() != null && !byCode.containsKey(row.parentCode())) {
                problems.add(row.code() + " has an unknown parent " + row.parentCode());
            }
        }

        for (Row row : rows) {
            Set<String> path = new HashSet<>();
            Row current = row;
            while (current != null && current.parentCode() != null) {
                if (!path.add(current.code())) {
                    problems.add(row.code() + " is part of a parent cycle");
                    break;
                }
                current = byCode.get(current.parentCode());
            }
        }
        return problems;
    }
}
