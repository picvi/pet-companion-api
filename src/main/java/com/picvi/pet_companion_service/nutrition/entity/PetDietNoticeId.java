package com.picvi.pet_companion_service.nutrition.entity;

import java.io.Serializable;
import java.util.UUID;

public record PetDietNoticeId(UUID petId, String code) implements Serializable {
}
