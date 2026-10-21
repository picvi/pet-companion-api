package com.picvi.pet_companion_service.nutrition.repository;

import com.picvi.pet_companion_service.nutrition.domain.PetFoodStatus;
import com.picvi.pet_companion_service.nutrition.entity.PetFood;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PetFoodRepository extends JpaRepository<PetFood, UUID> {

    /** Foods given on a day: started by then and not ended yet (ended_on is the first day no longer given). */
    @EntityGraph(attributePaths = "product")
    @Query("""
            select f from PetFood f
            where f.petId = :petId
              and f.status <> :excluded
              and f.startedOn <= :date
              and (f.endedOn is null or f.endedOn > :date)
            order by f.startedOn, f.id
            """)
    List<PetFood> findGivenOn(@Param("petId") UUID petId, @Param("date") LocalDate date, @Param("excluded") PetFoodStatus excluded);

    @EntityGraph(attributePaths = "product")
    Optional<PetFood> findByIdAndPetId(UUID id, UUID petId);

    @EntityGraph(attributePaths = "product")
    Optional<PetFood> findByPetIdAndClientId(UUID petId, UUID clientId);

    /** Foods given on some day between two dates. */
    @EntityGraph(attributePaths = "product")
    @Query("""
            select f from PetFood f
            where f.petId = :petId
              and f.status <> :excluded
              and f.startedOn <= :to
              and (f.endedOn is null or f.endedOn > :from)
            order by f.startedOn, f.id
            """)
    List<PetFood> findOverlapping(@Param("petId") UUID petId, @Param("from") LocalDate from, @Param("to") LocalDate to,
                                  @Param("excluded") PetFoodStatus excluded);
}
