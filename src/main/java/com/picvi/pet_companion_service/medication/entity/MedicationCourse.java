package com.picvi.pet_companion_service.medication.entity;

import com.picvi.pet_companion_service.medication.domain.CourseInput;
import com.picvi.pet_companion_service.medication.domain.CoursePlan;
import com.picvi.pet_companion_service.medication.domain.DoseSchedule;
import com.picvi.pet_companion_service.medication.domain.DoseUnit;
import com.picvi.pet_companion_service.medication.domain.WithFood;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "medication_course")
public class MedicationCourse {
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
    private String name;

    @Getter
    @Column(name = "amount_hundredths", nullable = false)
    private int amountHundredths;

    @Getter
    @Enumerated(EnumType.STRING)
    private DoseUnit unit;

    @Getter
    @Enumerated(EnumType.STRING)
    private DoseSchedule schedule;

    @Getter
    @Column(name = "every_days")
    private Integer everyDays;

    @ElementCollection
    @CollectionTable(name = "medication_dose_time", joinColumns = @JoinColumn(name = "course_id"))
    @Column(name = "minute_of_day")
    @BatchSize(size = 100)
    private Set<Integer> times = new HashSet<>();

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(name = "with_food")
    private WithFood withFood;

    @Getter
    @Column(name = "starts_on", nullable = false)
    private LocalDate startsOn;

    @Getter
    @Column(name = "start_minute", nullable = false)
    private int startMinute;

    @Getter
    @Column(name = "ends_on")
    private LocalDate endsOn;

    @Getter
    @Column(name = "ends_minute")
    private Integer endsMinute;

    @Getter
    @Column(name = "stopped_on")
    private LocalDate stoppedOn;

    @Getter
    @Column(name = "stopped_minute")
    private Integer stoppedMinute;

    @Getter
    @Column(nullable = false)
    private boolean reminders;

    @Version
    @Getter
    private long version;

    @Getter
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Getter
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public MedicationCourse() {}

    public MedicationCourse(UUID petId, UUID clientId, CourseInput input, boolean reminders, Instant now) {
        this.petId = petId;
        this.clientId = clientId;
        this.createdAt = now;
        this.replace(input, reminders, now);
    }

    public void replace(CourseInput input, boolean reminders, Instant now) {
        this.name = input.name().trim();
        this.amountHundredths = input.amountHundredths();
        this.unit = input.unit();
        this.schedule = input.schedule();
        this.everyDays = input.everyDays();
        this.times.clear();
        if (input.times() != null) {
            this.times.addAll(input.times());
        }
        this.withFood = input.withFood();
        this.startsOn = input.startsOn();
        this.startMinute = input.startMinute();
        this.endsOn = input.endsOn();
        this.endsMinute = input.endsMinute();
        this.reminders = reminders;
        this.updatedAt = now;
    }

    public void stop(LocalDate on, int minute, Instant now) {
        if (this.stoppedOn != null) {
            return;
        }
        this.stoppedOn = on;
        this.stoppedMinute = minute;
        this.updatedAt = now;
    }

    public CoursePlan plan() {
        return new CoursePlan(this.schedule, this.everyDays, this.times.stream().toList(), this.startsOn, this.startMinute,
                this.endsOn, this.endsMinute, this.stoppedOn, this.stoppedMinute);
    }

    public Set<Integer> getTimes() {
        return Set.copyOf(this.times);
    }
}
