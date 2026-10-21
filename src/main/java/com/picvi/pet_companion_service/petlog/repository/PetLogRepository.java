package com.picvi.pet_companion_service.petlog.repository;

import com.picvi.pet_companion_service.petlog.domain.LogType;
import com.picvi.pet_companion_service.petlog.entity.PetLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PetLogRepository extends JpaRepository<PetLog, UUID> {

    Optional<PetLog> findByPetIdAndClientId(UUID petId, UUID clientId);

    Optional<PetLog> findByIdAndPetId(UUID id, UUID petId);

    List<PetLog> findAllByPetIdAndLocalDateOrderByMinuteOfDayAsc(UUID petId, LocalDate localDate);

    @Query("""
            select l from PetLog l
            where l.pet.id = :petId
              and l.type in :types
              and (l.localDate < :date
                   or (l.localDate = :date and l.minuteOfDay < :minute)
                   or (l.localDate = :date and l.minuteOfDay = :minute and l.id < :id))
            order by l.localDate desc, l.minuteOfDay desc, l.id desc
            """)
    List<PetLog> findPageBefore(UUID petId, Collection<LogType> types, LocalDate date, int minute, UUID id, Pageable pageable);

    @Query("""
            select l from PetLog l
            where l.pet.id = :petId
              and (l.type in :types
                   or (l.type = com.picvi.pet_companion_service.petlog.domain.LogType.TOILET and size(l.flags) > 0))
              and (l.localDate < :date
                   or (l.localDate = :date and l.minuteOfDay < :minute)
                   or (l.localDate = :date and l.minuteOfDay = :minute and l.id < :id))
            order by l.localDate desc, l.minuteOfDay desc, l.id desc
            """)
    List<PetLog> findPageBeforeIncludingFlaggedToilet(UUID petId, Collection<LogType> types, LocalDate date, int minute, UUID id, Pageable pageable);

    List<PetLog> findAllByPetIdAndLocalDateGreaterThanEqual(UUID petId, LocalDate from);

    @Query("""
            select l from PetLog l
            where l.pet.id = :petId and l.localDate between :from and :to
            order by l.localDate asc, l.minuteOfDay asc, l.id asc
            """)
    List<PetLog> findAllByPetIdAndLocalDateBetweenOrderByLocalDateAscMinuteOfDayAsc(UUID petId, LocalDate from, LocalDate to);

    @Query("""
            select l from PetLog l
            where l.pet.id = :petId and l.type = :type and l.localDate <= :date
            order by l.localDate desc, l.minuteOfDay desc, l.id desc
            """)
    List<PetLog> findByPetIdAndTypeAndLocalDateLessThanEqualOrderByLocalDateDescMinuteOfDayDesc(UUID petId, LogType type, LocalDate date, Pageable pageable);

    @Query("""
            select l from PetLog l
            where l.pet.id = :petId
              and l.type = :type
              and (l.localDate < :date or (l.localDate = :date and l.minuteOfDay <= :minute))
            order by l.localDate desc, l.minuteOfDay desc, l.id desc
            """)
    List<PetLog> findAtOrBefore(UUID petId, LogType type, LocalDate date, int minute, Pageable pageable);

    List<PetLog> findByPetIdAndTypeOrderByLocalDateDescMinuteOfDayDesc(UUID petId, LogType type, Pageable pageable);

    Optional<PetLog> findFirstByPetIdAndTypeOrderByLocalDateDescMinuteOfDayDesc(UUID petId, LogType type);

    @Query("""
            select count(l) from PetLog l, PetLogChangeRow c
            where c.logId = l.id
              and c.change = com.picvi.pet_companion_service.petlog.domain.LogChange.CREATED
              and l.pet.id = :petId
              and l.type = com.picvi.pet_companion_service.petlog.domain.LogType.MEAL
              and c.changedAt > :since
            """)
    long countMealsCreatedSince(UUID petId, OffsetDateTime since);
}
