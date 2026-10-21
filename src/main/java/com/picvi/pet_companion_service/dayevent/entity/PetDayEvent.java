package com.picvi.pet_companion_service.dayevent.entity;

import com.picvi.pet_companion_service.dayevent.domain.DayEventType;
import com.picvi.pet_companion_service.dayevent.domain.EventDefinition;
import com.picvi.pet_companion_service.dayevent.domain.Recurrence;
import com.picvi.pet_companion_service.pet.entity.Pet;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.util.UUID;
import java.time.Instant;

@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "pet_day_event")
public class PetDayEvent {
    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Getter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    @Setter
    @Getter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DayEventType type;

    @Setter
    @Getter
    private String title;

    @Setter
    @Getter
    @Column(name = "local_date", nullable = false)
    private LocalDate localDate;

    @Setter
    @Getter
    @Column(name = "start_minute", nullable = false)
    private int startMinute;

    @Setter
    @Getter
    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Setter
    @Getter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Recurrence recurrence;

    @Setter
    @Getter
    @Column(name = "plan_around", nullable = false)
    private boolean planAround;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public PetDayEvent() {}

    public PetDayEvent(Pet pet) {
        this.pet = pet;
    }

    public EventDefinition toDefinition() {
        return new EventDefinition(this.id, this.type, this.title, this.localDate, this.startMinute,
                this.durationMinutes, this.recurrence, this.planAround);
    }
}
