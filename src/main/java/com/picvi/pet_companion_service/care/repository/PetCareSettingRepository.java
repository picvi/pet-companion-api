package com.picvi.pet_companion_service.care.repository;

import com.picvi.pet_companion_service.care.entity.PetCareSetting;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PetCareSettingRepository extends JpaRepository<PetCareSetting, UUID> {

    @EntityGraph(attributePaths = "item")
    List<PetCareSetting> findAllByPetId(UUID petId);

    Optional<PetCareSetting> findByPetIdAndItemCode(UUID petId, String itemCode);
}
