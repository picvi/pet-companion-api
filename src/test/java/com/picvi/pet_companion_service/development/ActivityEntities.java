package com.picvi.pet_companion_service.development;

import com.picvi.pet_companion_service.development.domain.ActivityDefinition;
import com.picvi.pet_companion_service.development.entity.DevelopmentActivity;
import org.springframework.test.util.ReflectionTestUtils;

public final class ActivityEntities {
    private ActivityEntities() {}

    public static DevelopmentActivity from(long id, ActivityDefinition definition) {
        DevelopmentActivity entity = new DevelopmentActivity();
        ReflectionTestUtils.setField(entity, "id", id);
        ReflectionTestUtils.setField(entity, "code", definition.code());
        ReflectionTestUtils.setField(entity, "kind", definition.kind());
        ReflectionTestUtils.setField(entity, "category", definition.category());
        ReflectionTestUtils.setField(entity, "startAgeWeeks", definition.startWeek());
        ReflectionTestUtils.setField(entity, "endAgeWeeks", definition.endWeek());
        ReflectionTestUtils.setField(entity, "difficulty", definition.difficulty());
        ReflectionTestUtils.setField(entity, "vaccinationRequirement", definition.vaccinationRequirement());
        ReflectionTestUtils.setField(entity, "repeatEveryDays", definition.repeatEveryDays());
        ReflectionTestUtils.setField(entity, "sortOrder", definition.sortOrder());
        ReflectionTestUtils.setField(entity, "active", true);
        return entity;
    }
}
