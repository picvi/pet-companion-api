package com.picvi.pet_companion_service.rhythm.repository;

import com.picvi.pet_companion_service.rhythm.entity.PetRhythmSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PetRhythmSettingsRepository extends JpaRepository<PetRhythmSettings, UUID> {

    Optional<PetRhythmSettings> findByPetId(UUID petId);
}
