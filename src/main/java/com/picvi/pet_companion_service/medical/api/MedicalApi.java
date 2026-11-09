package com.picvi.pet_companion_service.medical.api;

import com.picvi.pet_companion_service.medical.domain.VisitOutcome;
import com.picvi.pet_companion_service.medical.domain.VisitStatus;
import com.picvi.pet_companion_service.medical.entity.VetVisit;
import com.picvi.pet_companion_service.medical.repository.VetVisitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** What other modules may ask the vet visit notebook. */
@Component
@RequiredArgsConstructor
public class MedicalApi {
    private final VetVisitRepository repository;

    public record VisitSnapshot(UUID id, LocalDate visitedOn, List<VisitOutcome> outcomes, String clinic) {
        static VisitSnapshot of(VetVisit visit) {
            return new VisitSnapshot(visit.getId(), visit.getVisitedOn(),
                    visit.getOutcomes().stream().sorted(Comparator.naturalOrder()).toList(), visit.getClinic());
        }
    }

    /** The latest visit that happened on or before the given day. */
    @Transactional(readOnly = true)
    public Optional<VisitSnapshot> lastVisit(UUID petId, LocalDate before) {
        return this.repository.findFirstByPetIdAndStatusAndVisitedOnLessThanEqualOrderByVisitedOnDescCreatedAtDesc(petId, VisitStatus.DONE, before)
                .map(VisitSnapshot::of);
    }

    /** Visits that happened between two days, both included, newest first. */
    @Transactional(readOnly = true)
    public List<VisitSnapshot> visitsBetween(UUID petId, LocalDate from, LocalDate to) {
        return this.repository.findAllByPetIdAndStatusAndVisitedOnBetweenOrderByVisitedOnDescCreatedAtDesc(petId, VisitStatus.DONE, from, to).stream()
                .map(VisitSnapshot::of)
                .toList();
    }
}
