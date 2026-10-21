package com.picvi.pet_companion_service.lostdog.repository;

import com.picvi.pet_companion_service.lostdog.domain.LostStatus;
import com.picvi.pet_companion_service.lostdog.entity.LostReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LostReportRepository extends JpaRepository<LostReport, UUID> {
    Optional<LostReport> findFirstByPetIdAndStatus(UUID petId, LostStatus status);
    Optional<LostReport> findByPetIdAndClientId(UUID petId, UUID clientId);
    Optional<LostReport> findByIdAndPetId(UUID id, UUID petId);
    List<LostReport> findAllByPetIdAndStartedAtLessThanEqualOrderByStartedAtDesc(UUID petId, Instant through);
}
