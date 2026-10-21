package com.picvi.pet_companion_service.nutrition.dto;

import com.picvi.pet_companion_service.nutrition.domain.FoodKind;
import com.picvi.pet_companion_service.nutrition.domain.IngredientCode;
import com.picvi.pet_companion_service.nutrition.domain.IngredientKind;

import java.time.LocalDate;
import java.util.List;

/** Proteins (and carbohydrates) the dog has eaten. lastOn is null while it is still being given. */
public record ProteinExposureDto(IngredientCode code,
                                 IngredientKind sourceKind,
                                 List<FoodKind> kinds,
                                 LocalDate firstOn,
                                 LocalDate lastOn,
                                 boolean hydrolysed) {
}
