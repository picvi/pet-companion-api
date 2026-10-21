package com.picvi.pet_companion_service.triage.repository;

import com.picvi.pet_companion_service.triage.domain.CheckStatus;
import com.picvi.pet_companion_service.triage.entity.PetCheck;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PetCheckRepository extends JpaRepository<PetCheck, UUID> {
    Optional<PetCheck> findByPetIdAndClientId(UUID petId, UUID clientId);
    Optional<PetCheck> findByIdAndPetId(UUID id, UUID petId);
    List<PetCheck> findAllByPetIdAndOccurredOnBetweenOrderByOccurredOnAscMinuteOfDayAsc(UUID petId, LocalDate from, LocalDate to);
    List<PetCheck> findAllByPetIdAndStatusOrderByOccurredOnDescMinuteOfDayDesc(UUID petId, CheckStatus status);
}
