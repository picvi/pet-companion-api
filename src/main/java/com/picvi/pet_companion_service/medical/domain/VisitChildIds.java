package com.picvi.pet_companion_service.medical.domain;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Client ids for what a visit creates in other modules, so a retried request finds the first result instead of writing again. */
public final class VisitChildIds {
    private VisitChildIds() {
    }

    public static UUID of(UUID parentClientId, String role, int index) {
        return UUID.nameUUIDFromBytes((parentClientId + ":" + role + ":" + index).getBytes(StandardCharsets.UTF_8));
    }
}
