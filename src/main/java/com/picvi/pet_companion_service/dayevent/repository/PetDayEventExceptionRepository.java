package com.picvi.pet_companion_service.dayevent.repository;

import com.picvi.pet_companion_service.dayevent.entity.PetDayEvent;
import com.picvi.pet_companion_service.dayevent.entity.PetDayEventException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PetDayEventExceptionRepository extends JpaRepository<PetDayEventException, UUID> {

    Optional<PetDayEventException> findByEventIdAndLocalDate(UUID eventId, LocalDate localDate);

    List<PetDayEventException> findAllByEventInAndLocalDate(Collection<PetDayEvent> events, LocalDate localDate);
}
