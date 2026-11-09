package com.picvi.pet_companion_service.dayevent.repository;

import com.picvi.pet_companion_service.dayevent.entity.PetDayEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PetDayEventRepository extends JpaRepository<PetDayEvent, UUID> {

    Optional<PetDayEvent> findByIdAndPetId(UUID id, UUID petId);

    @Query("""
            select e from PetDayEvent e
            where e.pet.id = :petId and e.localDate <= :date
              and (e.recurrence = com.picvi.pet_companion_service.dayevent.domain.Recurrence.WEEKLY or e.localDate = :date)
            """)
    List<PetDayEvent> findPossiblyOccurringOn(UUID petId, LocalDate date);

    List<PetDayEvent> findAllByPetIdOrderByLocalDateAscStartMinuteAsc(UUID petId);
}
