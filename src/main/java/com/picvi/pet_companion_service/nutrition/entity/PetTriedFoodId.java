package com.picvi.pet_companion_service.nutrition.entity;

import java.io.Serializable;
import java.util.UUID;

public record PetTriedFoodId(UUID petId, String foodCode) implements Serializable {
}
