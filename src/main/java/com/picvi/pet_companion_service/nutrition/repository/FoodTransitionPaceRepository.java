package com.picvi.pet_companion_service.nutrition.repository;

import com.picvi.pet_companion_service.nutrition.entity.FoodTransitionPaceRow;
import com.picvi.pet_companion_service.nutrition.entity.FoodTransitionPaceRowId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodTransitionPaceRepository extends JpaRepository<FoodTransitionPaceRow, FoodTransitionPaceRowId> {

    List<FoodTransitionPaceRow> findAllByPaceAndStatusOrderByStepOrderAsc(String pace, String status);
}
