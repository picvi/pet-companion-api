package com.picvi.pet_companion_service.nutrition.dto;

import com.picvi.pet_companion_service.nutrition.domain.Completeness;
import com.picvi.pet_companion_service.nutrition.domain.FoodForm;
import com.picvi.pet_companion_service.nutrition.domain.FoodKind;
import com.picvi.pet_companion_service.nutrition.domain.LifeStage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/** "Add it myself". {@code proteinNotSure} is the owner saying they can't tell the main protein: no MAIN ingredient may be sent then. */
public record AddFoodProductRequest(@Size(max = 80) String brand,
                                    @NotBlank @Size(max = 120) String productName,
                                    @Size(max = 80) String flavour,
                                    @NotNull FoodForm form,
                                    @NotNull FoodKind kind,
                                    LifeStage lifeStage,
                                    Completeness complete,
                                    Boolean proteinNotSure,
                                    @Valid @Size(max = 20) List<IngredientDto> ingredients,
                                    UUID petId,
                                    Boolean confirmAllergy) {

    public boolean confirmed() {
        return Boolean.TRUE.equals(this.confirmAllergy);
    }

    public boolean notSure() {
        return Boolean.TRUE.equals(this.proteinNotSure);
    }
}
