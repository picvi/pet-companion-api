package com.picvi.pet_companion_service.nutrition.repository;

import com.picvi.pet_companion_service.nutrition.entity.FoodProductIngredient;
import com.picvi.pet_companion_service.nutrition.entity.FoodProductIngredientId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface FoodProductIngredientRepository extends JpaRepository<FoodProductIngredient, FoodProductIngredientId> {

    List<FoodProductIngredient> findAllByProductIdIn(Collection<UUID> productIds);

    void deleteAllByProductId(UUID productId);
}
