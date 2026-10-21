package com.picvi.pet_companion_service.nutrition.repository;

import com.picvi.pet_companion_service.nutrition.entity.IngredientSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IngredientSourceRepository extends JpaRepository<IngredientSource, String> {

    List<IngredientSource> findAllByOrderBySortOrderAsc();
}
