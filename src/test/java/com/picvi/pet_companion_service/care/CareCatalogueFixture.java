package com.picvi.pet_companion_service.care;

import com.picvi.pet_companion_service.care.domain.CareCategory;
import com.picvi.pet_companion_service.care.domain.CareItemDefinition;
import com.picvi.pet_companion_service.care.domain.CareRequirement;
import com.picvi.pet_companion_service.care.domain.CareStepDefinition;
import com.picvi.pet_companion_service.care.domain.StepKind;

import java.util.List;

public final class CareCatalogueFixture {
    public static final CareItemDefinition RABIES = new CareItemDefinition(
            "RABIES", CareCategory.VACCINE, CareRequirement.REQUIRED, false, 3, 30, 12,
            List.of(step(StepKind.SERIES, null, null, null), step(StepKind.REPEAT, null, null, 365)));

    public static final CareItemDefinition DHPPI = new CareItemDefinition(
            "DHPPI", CareCategory.VACCINE, CareRequirement.RECOMMENDED, false, null, null, null,
            List.of(
                    step(StepKind.SERIES, 6, null, null),
                    step(StepKind.SERIES, 9, null, 21),
                    step(StepKind.SERIES, 16, null, 21),
                    step(StepKind.BOOSTER, null, null, 365),
                    step(StepKind.REPEAT, null, null, 1095)));

    public static final CareItemDefinition KENNEL_COUGH = new CareItemDefinition(
            "KENNEL_COUGH", CareCategory.VACCINE, CareRequirement.OPTIONAL, false, null, null, null,
            List.of(step(StepKind.SERIES, 8, null, null), step(StepKind.REPEAT, null, null, 365)));

    public static final CareItemDefinition DEWORMING = new CareItemDefinition(
            "DEWORMING", CareCategory.PARASITE, CareRequirement.RECOMMENDED, true, null, null, null,
            List.of(step(StepKind.REPEAT, 2, 26, 30), step(StepKind.REPEAT, null, null, 90)));

    public static final CareItemDefinition TICK_PROTECTION = new CareItemDefinition(
            "TICK_PROTECTION", CareCategory.PARASITE, CareRequirement.RECOMMENDED, true, null, null, null,
            List.of(new CareStepDefinition(StepKind.REPEAT, 8, null, 28, 3, 11)));

    private CareCatalogueFixture() {}

    private static CareStepDefinition step(StepKind kind, Integer minAgeWeeks, Integer maxAgeWeeks, Integer gapDays) {
        return new CareStepDefinition(kind, minAgeWeeks, maxAgeWeeks, gapDays, null, null);
    }
}
