package com.picvi.pet_companion_service.nutrition.dto;

import com.picvi.pet_companion_service.nutrition.api.DayPortions;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** One call that feeds the Food screen, the Health row and the Now card. */
public record DietResponse(LocalDate date,
                           int mealsPerDay,
                           List<PetFoodDto> foods,
                           DayPortions today,
                           DayPortions tomorrow,
                           FoodChangeDto change,
                           List<ProteinExposureDto> proteins,
                           List<UUID> unknownProteinFoodIds,
                           DietNoticesDto notices) {
}
