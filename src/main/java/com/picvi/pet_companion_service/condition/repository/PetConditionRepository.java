package com.picvi.pet_companion_service.condition.repository;

import com.picvi.pet_companion_service.condition.entity.PetCondition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PetConditionRepository extends JpaRepository<PetCondition, UUID> {

    List<PetCondition> findAllByPetIdOrderByCreatedAtAsc(UUID petId);

    Optional<PetCondition> findByIdAndPetId(UUID id, UUID petId);
}
