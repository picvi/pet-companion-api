package com.picvi.pet_companion_service.nutrition.repository;

import com.picvi.pet_companion_service.nutrition.entity.FoodProductChange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface FoodProductChangeRepository extends JpaRepository<FoodProductChange, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE food_product_change SET changed_by = NULL WHERE changed_by = :userId", nativeQuery = true)
    void clearChangedBy(@Param("userId") UUID userId);
}
