package com.picvi.pet_companion_service.nutrition.repository;

import com.picvi.pet_companion_service.nutrition.entity.PetTriedFood;
import com.picvi.pet_companion_service.nutrition.entity.PetTriedFoodId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PetTriedFoodRepository extends JpaRepository<PetTriedFood, PetTriedFoodId> {

    List<PetTriedFood> findAllByPetIdOrderByTriedOnDescFoodCodeAsc(UUID petId);

    Optional<PetTriedFood> findByPetIdAndFoodCode(UUID petId, String foodCode);
}
