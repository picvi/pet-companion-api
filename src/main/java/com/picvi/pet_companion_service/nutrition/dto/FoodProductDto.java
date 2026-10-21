package com.picvi.pet_companion_service.nutrition.dto;

import com.picvi.pet_companion_service.nutrition.domain.AllergyMatch;
import com.picvi.pet_companion_service.nutrition.domain.BreedSize;
import com.picvi.pet_companion_service.nutrition.domain.Completeness;
import com.picvi.pet_companion_service.nutrition.domain.FoodForm;
import com.picvi.pet_companion_service.nutrition.domain.FoodKind;
import com.picvi.pet_companion_service.nutrition.domain.IngredientRole;
import com.picvi.pet_companion_service.nutrition.domain.LifeStage;
import com.picvi.pet_companion_service.nutrition.domain.ProductStatus;
import com.picvi.pet_companion_service.nutrition.domain.ProteinStatus;
import com.picvi.pet_companion_service.nutrition.entity.FoodProduct;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record FoodProductDto(UUID id,
                             String brand,
                             String productLine,
                             String productName,
                             String flavour,
                             FoodForm form,
                             FoodKind kindHint,
                             LifeStage lifeStage,
                             BreedSize breedSize,
                             Completeness complete,
                             ProteinStatus proteinStatus,
                             ProductStatus status,
                             List<IngredientDto> ingredients,
                             List<AllergyMatch> allergyMatches) {

    public static FoodProductDto of(FoodProduct product, List<IngredientDto> ingredients) {
        return of(product, ingredients, List.of());
    }

    public static FoodProductDto of(FoodProduct product, List<IngredientDto> ingredients, List<AllergyMatch> allergyMatches) {
        List<IngredientDto> sorted = ingredients.stream()
                .sorted(Comparator.comparing((IngredientDto i) -> i.role() == IngredientRole.MAIN ? 0 : 1)
                        .thenComparing(i -> i.code().ordinal()))
                .toList();
        return new FoodProductDto(product.getId(), product.getBrand(), product.getProductLine(), product.getProductName(),
                product.getFlavour(), product.getForm(), product.getKindHint(), product.getLifeStage(), product.getBreedSize(),
                product.getComplete(), product.getProteinStatus(), product.getStatus(), sorted, allergyMatches);
    }
}
