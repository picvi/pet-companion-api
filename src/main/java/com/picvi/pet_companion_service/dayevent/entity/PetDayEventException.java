package com.picvi.pet_companion_service.dayevent.entity;

import com.picvi.pet_companion_service.dayevent.domain.OccurrenceChange;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "pet_day_event_exception")
public class PetDayEventException {
    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Getter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private PetDayEvent event;

    @Getter
    @Column(name = "local_date", nullable = false)
    private LocalDate localDate;

    @Setter
    @Getter
    @Column(nullable = false)
    private boolean skipped;

    @Setter
    @Getter
    @Column(name = "start_minute")
    private Integer startMinute;

    public PetDayEventException() {}

    public PetDayEventException(PetDayEvent event, LocalDate localDate) {
        this.event = event;
        this.localDate = localDate;
    }

    public OccurrenceChange toChange() {
        return new OccurrenceChange(this.event.getId(), this.localDate, this.skipped, this.startMinute);
    }
}
