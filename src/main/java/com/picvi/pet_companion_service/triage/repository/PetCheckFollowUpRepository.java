package com.picvi.pet_companion_service.triage.repository;

import com.picvi.pet_companion_service.triage.entity.PetCheckFollowUp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PetCheckFollowUpRepository extends JpaRepository<PetCheckFollowUp, UUID> {
    Optional<PetCheckFollowUp> findByCheckIdAndClientId(UUID checkId, UUID clientId);
    List<PetCheckFollowUp> findAllByCheckIdOrderByAnsweredOnAscMinuteOfDayAsc(UUID checkId);
    long countByCheckIdAndAnswer(UUID checkId, com.picvi.pet_companion_service.triage.domain.FollowUpAnswer answer);
}
