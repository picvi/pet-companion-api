package com.picvi.pet_companion_service.development.repository;

import com.picvi.pet_companion_service.development.domain.Outcome;
import com.picvi.pet_companion_service.development.entity.PetActivityLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PetActivityLogRepository extends JpaRepository<PetActivityLog, UUID> {

    List<PetActivityLog> findAllByPetIdAndActivityIdAndPerformedOn(UUID petId, Long activityId, LocalDate performedOn);

    Optional<PetActivityLog> findFirstByPetIdAndActivityIdAndPerformedOnBeforeOrderByPerformedOnDescCreatedAtDesc(
            UUID petId, Long activityId, LocalDate performedOn);

    List<PetActivityLog> findByPetIdAndActivityIdAndDurationSecondsNotNullOrderByPerformedOnDescCreatedAtDesc(
            UUID petId, Long activityId, Pageable pageable);

    List<PetActivityLog> findAllByPetIdAndActivityIdAndDurationSecondsNotNullOrderByPerformedOnAscCreatedAtAsc(
            UUID petId, Long activityId);

    Optional<PetActivityLog> findFirstByPetIdAndActivityIdAndOutcomeAndDurationSecondsNotNullOrderByDurationSecondsDesc(
            UUID petId, Long activityId, Outcome outcome);

    void deleteAllByPetIdAndActivityId(UUID petId, Long activityId);

    Optional<PetActivityLog> findByIdAndPetIdAndActivityId(UUID id, UUID petId, Long activityId);

    @Query("""
            select a from PetActivityLog a join fetch a.activity
            where a.pet.id = :petId
              and (a.performedOn < :date
                   or (a.performedOn = :date and (:includeWholeDay = true or a.id < :idBefore)))
            order by a.performedOn desc, a.id desc
            """)
    List<PetActivityLog> findTimelinePage(UUID petId, LocalDate date, boolean includeWholeDay, UUID idBefore, Pageable pageable);
}
