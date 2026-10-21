package com.picvi.pet_companion_service.medical.entity;

import com.picvi.pet_companion_service.medical.domain.BriefHelpful;
import com.picvi.pet_companion_service.medical.domain.VisitOutcome;
import com.picvi.pet_companion_service.medical.domain.VisitReason;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "pet_vet_visit")
public class VetVisit {
    @Id
    @Getter
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Getter
    @Column(name = "pet_id", nullable = false, updatable = false)
    private UUID petId;

    @Getter
    @Column(name = "client_id", nullable = false, updatable = false)
    private UUID clientId;

    @Getter
    @Column(name = "visited_on", nullable = false)
    private LocalDate visitedOn;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VisitReason reason;

    @Getter
    @Convert(converter = VisitOutcomesConverter.class)
    @Column(nullable = false)
    private Set<VisitOutcome> outcomes = EnumSet.noneOf(VisitOutcome.class);

    @Getter
    private String clinic;

    @Getter
    private String note;

    @Getter
    @Setter
    @Column(name = "next_check_event_id")
    private UUID nextCheckEventId;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(name = "brief_helpful")
    private BriefHelpful briefHelpful;

    @Version
    @Getter
    private long version;

    @Getter
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Getter
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public VetVisit() {}

    public VetVisit(UUID petId, UUID clientId, LocalDate visitedOn, VisitReason reason, Set<VisitOutcome> outcomes, String clinic,
                    String note, BriefHelpful briefHelpful, Instant now) {
        this.petId = petId;
        this.clientId = clientId;
        this.createdAt = now;
        this.replace(visitedOn, reason, outcomes, clinic, note, briefHelpful, now);
    }

    public void replace(LocalDate visitedOn, VisitReason reason, Set<VisitOutcome> outcomes, String clinic, String note,
                        BriefHelpful briefHelpful, Instant now) {
        this.visitedOn = visitedOn;
        this.reason = reason;
        this.outcomes = outcomes.isEmpty() ? EnumSet.noneOf(VisitOutcome.class) : EnumSet.copyOf(outcomes);
        this.clinic = clinic;
        this.note = note;
        this.briefHelpful = briefHelpful;
        this.updatedAt = now;
    }
}
