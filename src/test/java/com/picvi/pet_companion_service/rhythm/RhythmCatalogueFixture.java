package com.picvi.pet_companion_service.rhythm;

import com.picvi.pet_companion_service.breeds.entity.SizeCategory;
import com.picvi.pet_companion_service.rhythm.domain.Range;
import com.picvi.pet_companion_service.rhythm.domain.RhythmRule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class RhythmCatalogueFixture {
    private static final Path SEED = Path.of("src/main/resources/db/migration/R__rhythm_rules.sql");

    private RhythmCatalogueFixture() {}

    public static List<RhythmRule> load() {
        try {
            return Files.readAllLines(SEED).stream()
                    .map(String::strip)
                    .filter(line -> line.startsWith("(") && line.matches(".*\\d.*'\\)[,;]"))
                    .map(RhythmCatalogueFixture::parse)
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static RhythmRule parse(String line) {
        String[] values = line.substring(1, line.length() - 2).split(",\\s*(?=(?:[^']*'[^']*')*[^']*$)");
        return new RhythmRule(
                Integer.parseInt(values[0]),
                Integer.parseInt(values[1]),
                values[2].equals("NULL") ? null : SizeCategory.valueOf(values[2].replace("'", "")),
                new Range(Integer.parseInt(values[3]), Integer.parseInt(values[4]), Integer.parseInt(values[5])),
                new Range(Integer.parseInt(values[6]), Integer.parseInt(values[7]), Integer.parseInt(values[8])),
                new Range(Integer.parseInt(values[9]), Integer.parseInt(values[10]), Integer.parseInt(values[11])),
                Integer.parseInt(values[12]),
                Integer.parseInt(values[13]),
                Boolean.parseBoolean(values[14]),
                Boolean.parseBoolean(values[15]),
                List.of(values[16].replace("'", "").split(",")));
    }
}
