package com.picvi.pet_companion_service.medical.entity;

import com.picvi.pet_companion_service.medical.domain.BriefHelpful;
import com.picvi.pet_companion_service.medical.domain.VisitLogRole;
import com.picvi.pet_companion_service.medical.domain.VisitOutcome;
import com.picvi.pet_companion_service.medical.domain.VisitStatus;
import com.picvi.pet_companion_service.medical.domain.VisitTestKind;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VisitStatus status = VisitStatus.DONE;

    @Getter
    @Column(name = "visited_on", nullable = false)
    private LocalDate visitedOn;

    @Getter
    @Column(name = "scheduled_minute")
    private Integer scheduledMinute;

    @Getter
    @Column(name = "prep_note")
    private String prepNote;

    @Getter
    @Column(name = "just_check", nullable = false)
    private boolean justCheck;

    @Getter
    @Setter
    @Column(name = "follow_up_of")
    private UUID followUpOf;

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
    @Column(name = "day_event_id")
    private UUID dayEventId;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(name = "brief_helpful")
    private BriefHelpful briefHelpful;

    @Getter
    @ElementCollection
    @CollectionTable(name = "pet_vet_visit_care_event", joinColumns = @JoinColumn(name = "visit_id"))
    @Column(name = "care_event_id")
    @BatchSize(size = 50)
    private Set<UUID> careEventIds = new HashSet<>();

    @Getter
    @ElementCollection
    @CollectionTable(name = "pet_vet_visit_log", joinColumns = @JoinColumn(name = "visit_id"))
    @MapKeyColumn(name = "log_id")
    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    @BatchSize(size = 50)
    private Map<UUID, VisitLogRole> logs = new HashMap<>();

    @Getter
    @ElementCollection
    @CollectionTable(name = "pet_vet_visit_course", joinColumns = @JoinColumn(name = "visit_id"))
    @Column(name = "course_id")
    @BatchSize(size = 50)
    private Set<UUID> courseIds = new HashSet<>();

    @Getter
    @ElementCollection
    @CollectionTable(name = "pet_vet_visit_test", joinColumns = @JoinColumn(name = "visit_id"))
    @MapKeyColumn(name = "kind")
    @MapKeyEnumerated(EnumType.STRING)
    @Column(name = "waiting_for_results")
    @BatchSize(size = 50)
    private Map<VisitTestKind, Boolean> tests = new EnumMap<>(VisitTestKind.class);

    @Getter
    @ElementCollection
    @CollectionTable(name = "pet_vet_visit_procedure", joinColumns = @JoinColumn(name = "visit_id"))
    @BatchSize(size = 50)
    private List<VisitProcedure> procedures = new ArrayList<>();

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

    public static VetVisit planned(UUID petId, UUID clientId, LocalDate scheduledOn, int scheduledMinute, String clinic,
                                   String prepNote, Instant now) {
        VetVisit visit = new VetVisit();
        visit.petId = petId;
        visit.clientId = clientId;
        visit.status = VisitStatus.PLANNED;
        visit.createdAt = now;
        visit.replan(scheduledOn, scheduledMinute, clinic, prepNote, now);
        return visit;
    }

    public static VetVisit done(UUID petId, UUID clientId, LocalDate visitedOn, String clinic, Instant now) {
        VetVisit visit = new VetVisit();
        visit.petId = petId;
        visit.clientId = clientId;
        visit.status = VisitStatus.DONE;
        visit.visitedOn = visitedOn;
        visit.clinic = clinic;
        visit.createdAt = now;
        visit.updatedAt = now;
        return visit;
    }

    public void replan(LocalDate scheduledOn, int scheduledMinute, String clinic, String prepNote, Instant now) {
        this.visitedOn = scheduledOn;
        this.scheduledMinute = scheduledMinute;
        this.clinic = clinic;
        this.prepNote = prepNote;
        this.updatedAt = now;
    }

    public boolean isPlanned() {
        return this.status == VisitStatus.PLANNED;
    }

    /** Turns a plan into a visit that happened, or changes the own fields of one that did. */
    public void record(LocalDate visitedOn, String clinic, String note, boolean justCheck, Instant now) {
        this.status = VisitStatus.DONE;
        this.visitedOn = visitedOn;
        this.clinic = clinic;
        this.note = note;
        this.justCheck = justCheck;
        this.updatedAt = now;
    }

    public void replaceTests(Map<VisitTestKind, Boolean> tests) {
        this.tests.clear();
        this.tests.putAll(tests);
    }

    public void replaceProcedure(VisitProcedure procedure) {
        this.procedures.clear();
        if (procedure != null) {
            this.procedures.add(procedure);
        }
    }

    public void setOutcomes(Set<VisitOutcome> outcomes) {
        this.outcomes = EnumSet.noneOf(VisitOutcome.class);
        this.outcomes.addAll(outcomes);
    }

    public boolean hasLog(VisitLogRole role) {
        return this.logs.containsValue(role);
    }

    public boolean hasLinkedTreatment() {
        return !this.careEventIds.isEmpty() || !this.courseIds.isEmpty() || this.hasLog(VisitLogRole.CLINIC_MEDICINE);
    }
}
