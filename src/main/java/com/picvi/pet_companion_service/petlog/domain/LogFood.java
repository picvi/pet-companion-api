package com.picvi.pet_companion_service.petlog.domain;

import java.util.UUID;

/** One of the pet's foods in a logged meal; grams is null when the owner didn't weigh it. */
public record LogFood(UUID petFoodId, Integer grams) {
}
