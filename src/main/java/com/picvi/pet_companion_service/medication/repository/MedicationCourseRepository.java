package com.picvi.pet_companion_service.medication.repository;

import com.picvi.pet_companion_service.medication.entity.MedicationCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MedicationCourseRepository extends JpaRepository<MedicationCourse, UUID> {

    Optional<MedicationCourse> findByPetIdAndClientId(UUID petId, UUID clientId);

    Optional<MedicationCourse> findByIdAndPetId(UUID id, UUID petId);

    List<MedicationCourse> findAllByPetIdAndIdIn(UUID petId, Collection<UUID> ids);

    @Query("""
            select c from MedicationCourse c
            where c.petId = :petId
              and (c.endsOn is null or c.endsOn >= :since)
              and (c.stoppedOn is null or c.stoppedOn >= :since)
            """)
    List<MedicationCourse> findNotEndedBefore(UUID petId, LocalDate since);
}
