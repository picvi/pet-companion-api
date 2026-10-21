package com.picvi.pet_companion_service.nutrition.domain;

import com.picvi.pet_companion_service.nutrition.domain.ReferenceCodeChecker.Row;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReferenceCodeCheckerTest {
    private final ReferenceCodeChecker checker = new ReferenceCodeChecker();

    private List<Row> matchingRows() {
        return Arrays.stream(IngredientCode.values())
                .map(code -> new Row(code.name(), code.getKind(), code.parent() == null ? null : code.parent().name()))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    @Test
    void agreeingEnumAndTableHaveNoProblems() {
        assertThat(this.checker.check(this.matchingRows())).isEmpty();
    }

    @Test
    void enumValueMissingFromTableIsReported() {
        List<Row> rows = this.matchingRows();
        rows.removeIf(row -> row.code().equals("QUAIL"));

        assertThat(this.checker.check(rows)).anyMatch(problem -> problem.contains("QUAIL") && problem.contains("not in ingredient_source"));
    }

    @Test
    void tableRowMissingFromEnumIsReported() {
        List<Row> rows = this.matchingRows();
        rows.add(new Row("UNICORN", IngredientKind.PROTEIN, null));

        assertThat(this.checker.check(rows)).anyMatch(problem -> problem.contains("UNICORN") && problem.contains("not in the enum"));
    }

    @Test
    void differentKindOrParentIsReported() {
        List<Row> rows = this.matchingRows();
        rows.replaceAll(row -> row.code().equals("RICE") ? new Row("RICE", IngredientKind.PROTEIN, null) : row);
        rows.replaceAll(row -> row.code().equals("CHICKEN") ? new Row("CHICKEN", IngredientKind.PROTEIN, "FISH") : row);

        assertThat(this.checker.check(rows)).anyMatch(problem -> problem.contains("RICE"))
                .anyMatch(problem -> problem.contains("CHICKEN") && problem.contains("parent"));
    }

    @Test
    void enumParentsFormNoCycles() {
        for (IngredientCode code : IngredientCode.values()) {
            IngredientCode current = code;
            int steps = 0;
            while (current.parent() != null) {
                current = current.parent();
                assertThat(++steps).isLessThan(5);
            }
        }
    }
}
