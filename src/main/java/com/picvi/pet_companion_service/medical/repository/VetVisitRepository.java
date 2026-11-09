package com.picvi.pet_companion_service.medical.repository;

import com.picvi.pet_companion_service.medical.domain.VisitStatus;
import com.picvi.pet_companion_service.medical.entity.VetVisit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VetVisitRepository extends JpaRepository<VetVisit, UUID> {

    Optional<VetVisit> findByPetIdAndClientId(UUID petId, UUID clientId);

    Optional<VetVisit> findByIdAndPetId(UUID id, UUID petId);

    Optional<VetVisit> findByPetIdAndDayEventIdAndStatus(UUID petId, UUID dayEventId, VisitStatus status);

    Optional<VetVisit> findFirstByFollowUpOf(UUID visitId);

    List<VetVisit> findAllByPetIdAndStatusAndVisitedOnGreaterThanEqualOrderByVisitedOnAscScheduledMinuteAsc(
            UUID petId, VisitStatus status, LocalDate from);

    List<VetVisit> findAllByPetIdAndStatusAndVisitedOnBetweenOrderByVisitedOnDescCreatedAtDesc(
            UUID petId, VisitStatus status, LocalDate from, LocalDate to);

    Optional<VetVisit> findFirstByPetIdAndStatusAndVisitedOnLessThanEqualOrderByVisitedOnDescCreatedAtDesc(
            UUID petId, VisitStatus status, LocalDate date);
}
