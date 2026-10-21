package com.picvi.pet_companion_service.development;

import com.picvi.pet_companion_service.development.domain.ActivityCategory;
import com.picvi.pet_companion_service.development.domain.ActivityDefinition;
import com.picvi.pet_companion_service.development.domain.ActivityKind;
import com.picvi.pet_companion_service.development.domain.VaccinationRequirement;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class ActivityCatalogueFixture {
    private static final String CATALOGUE_RESOURCE = "/development_activities.csv";

    private ActivityCatalogueFixture() {}

    public static List<ActivityDefinition> load() {
        try (InputStream stream = ActivityCatalogueFixture.class.getResourceAsStream(CATALOGUE_RESOURCE);
             BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            return reader.lines()
                    .skip(1)
                    .filter(line -> !line.isBlank())
                    .map(ActivityCatalogueFixture::parseRow)
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static ActivityDefinition parseRow(String line) {
        String[] columns = line.split(",", -1);
        return new ActivityDefinition(
                columns[0],
                ActivityKind.valueOf(columns[1]),
                ActivityCategory.valueOf(columns[2]),
                Integer.parseInt(columns[3]),
                Integer.parseInt(columns[4]),
                Integer.parseInt(columns[5]),
                VaccinationRequirement.valueOf(columns[6]),
                columns[7].isEmpty() ? null : Integer.valueOf(columns[7]),
                Integer.parseInt(columns[8])
        );
    }
}
