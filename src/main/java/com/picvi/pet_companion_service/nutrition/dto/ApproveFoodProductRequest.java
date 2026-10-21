package com.picvi.pet_companion_service.nutrition.dto;

import com.picvi.pet_companion_service.nutrition.domain.BreedSize;
import com.picvi.pet_companion_service.nutrition.domain.Completeness;
import com.picvi.pet_companion_service.nutrition.domain.FoodForm;
import com.picvi.pet_companion_service.nutrition.domain.FoodKind;
import com.picvi.pet_companion_service.nutrition.domain.LifeStage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** The admin's corrected version of an owner-added product. Everything is replaced by what is sent. */
public record ApproveFoodProductRequest(@Size(max = 80) String brand,
                                        @Size(max = 80) String productLine,
                                        @NotBlank @Size(max = 120) String productName,
                                        @Size(max = 80) String flavour,
                                        @NotNull FoodForm form,
                                        @NotNull FoodKind kind,
                                        LifeStage lifeStage,
                                        BreedSize breedSize,
                                        Completeness complete,
                                        Boolean proteinNotSure,
                                        @Valid @Size(max = 20) List<IngredientDto> ingredients,
                                        @Size(max = 300) String sourceUrl) {

    public boolean notSure() {
        return Boolean.TRUE.equals(this.proteinNotSure);
    }
}
