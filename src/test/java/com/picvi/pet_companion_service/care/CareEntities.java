package com.picvi.pet_companion_service.care;

import com.picvi.pet_companion_service.care.domain.CareItemDefinition;
import com.picvi.pet_companion_service.care.entity.CareEvent;
import com.picvi.pet_companion_service.care.entity.CareItem;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.UUID;

public final class CareEntities {
    private CareEntities() {}

    public static CareItem item(CareItemDefinition definition) {
        return item(definition, false);
    }

    public static CareItem item(CareItemDefinition definition, boolean drivesVaccinationStatus) {
        CareItem item = new CareItem();
        ReflectionTestUtils.setField(item, "drivesVaccinationStatus", drivesVaccinationStatus);
        ReflectionTestUtils.setField(item, "code", definition.code());
        ReflectionTestUtils.setField(item, "category", definition.category());
        ReflectionTestUtils.setField(item, "requirement", definition.requirement());
        ReflectionTestUtils.setField(item, "intervalEditable", definition.intervalEditable());
        ReflectionTestUtils.setField(item, "active", true);
        return item;
    }

    public static CareEvent event(CareItem item, LocalDate performedOn) {
        CareEvent event = new CareEvent(null, item, performedOn);
        ReflectionTestUtils.setField(event, "id", UUID.randomUUID());
        return event;
    }
}
