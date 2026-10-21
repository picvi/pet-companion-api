package com.picvi.pet_companion_service.care.repository;

import com.picvi.pet_companion_service.care.entity.CareEvent;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CareEventRepository extends JpaRepository<CareEvent, UUID> {

    @EntityGraph(attributePaths = "item")
    List<CareEvent> findAllByPetId(UUID petId);

    @EntityGraph(attributePaths = "item")
    List<CareEvent> findAllByPetIdAndItemCodeOrderByPerformedOnDesc(UUID petId, String itemCode);

    @EntityGraph(attributePaths = "item")
    Optional<CareEvent> findByIdAndPetId(UUID id, UUID petId);

    @Query("""
            select e from CareEvent e join fetch e.item
            where e.pet.id = :petId
              and (e.performedOn < :date
                   or (e.performedOn = :date and (:includeWholeDay = true or e.id < :idBefore)))
            order by e.performedOn desc, e.id desc
            """)
    List<CareEvent> findTimelinePage(UUID petId, LocalDate date, boolean includeWholeDay, UUID idBefore, Pageable pageable);
}
