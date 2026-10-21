package com.picvi.pet_companion_service.rhythm.entity;

import com.picvi.pet_companion_service.pet.entity.Pet;
import com.picvi.pet_companion_service.rhythm.domain.DaySettings;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pet_rhythm_settings")
public class PetRhythmSettings {
    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Getter
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    @Setter
    @Getter
    @Column(name = "wake_minute", nullable = false)
    private int wakeMinute;

    @Setter
    @Getter
    @Column(name = "bed_minute", nullable = false)
    private int bedMinute;

    @Setter
    @Getter
    @Column(name = "custom_awake_minutes")
    private Integer customAwakeMinutes;

    @Getter
    @Column(name = "created_on", nullable = false, updatable = false)
    private LocalDate createdOn;

    @Setter
    @Getter
    @Column(name = "band_note_seen_from_weeks")
    private Integer bandNoteSeenFromWeeks;

    @Setter
    @Getter
    @Column(name = "checkin_answered_on")
    private LocalDate checkinAnsweredOn;

    @Getter
    @OrderBy
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "pet_meal_time", joinColumns = @JoinColumn(name = "settings_id"))
    @Column(name = "meal_minute", nullable = false)
    private List<Integer> mealMinutes = new ArrayList<>();

    public PetRhythmSettings() {}

    public PetRhythmSettings(Pet pet, LocalDate createdOn, int bandFromWeeks) {
        this.pet = pet;
        this.createdOn = createdOn;
        this.bandNoteSeenFromWeeks = bandFromWeeks;
    }

    public DaySettings toDaySettings() {
        return new DaySettings(this.wakeMinute, this.bedMinute, List.copyOf(this.mealMinutes), this.customAwakeMinutes);
    }

    public void replaceMealMinutes(List<Integer> minutes) {
        this.mealMinutes.clear();
        this.mealMinutes.addAll(minutes);
    }
}
