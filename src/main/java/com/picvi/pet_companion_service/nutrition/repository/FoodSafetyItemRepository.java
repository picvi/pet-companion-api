package com.picvi.pet_companion_service.nutrition.repository;

import com.picvi.pet_companion_service.nutrition.entity.FoodSafetyItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodSafetyItemRepository extends JpaRepository<FoodSafetyItem, String> {

    List<FoodSafetyItem> findAllByOrderByCodeAsc();
}
