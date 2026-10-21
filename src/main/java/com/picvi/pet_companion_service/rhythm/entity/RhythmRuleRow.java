package com.picvi.pet_companion_service.rhythm.entity;

import com.picvi.pet_companion_service.breeds.entity.SizeCategory;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "rhythm_rule")
public class RhythmRuleRow {
    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter
    @Column(name = "from_weeks", nullable = false)
    private int fromWeeks;

    @Getter
    @Column(name = "to_weeks", nullable = false)
    private int toWeeks;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(name = "size_category")
    private SizeCategory sizeCategory;

    @Getter
    @Column(name = "awake_min", nullable = false)
    private int awakeMin;

    @Getter
    @Column(name = "awake_max", nullable = false)
    private int awakeMax;

    @Getter
    @Column(name = "awake_standard", nullable = false)
    private int awakeStandard;

    @Getter
    @Column(name = "nap_min", nullable = false)
    private int napMin;

    @Getter
    @Column(name = "nap_max", nullable = false)
    private int napMax;

    @Getter
    @Column(name = "nap_standard", nullable = false)
    private int napStandard;

    @Getter
    @Column(name = "meals_min", nullable = false)
    private int mealsMin;

    @Getter
    @Column(name = "meals_max", nullable = false)
    private int mealsMax;

    @Getter
    @Column(name = "meals_standard", nullable = false)
    private int mealsStandard;

    @Getter
    @Column(name = "toilet_interval_minutes", nullable = false)
    private int toiletIntervalMinutes;

    @Getter
    @Column(name = "night_trips", nullable = false)
    private int nightTrips;

    @Getter
    @Column(name = "meal_safety_note", nullable = false)
    private boolean mealSafetyNote;

    @Getter
    @Column(name = "soft_ground_play", nullable = false)
    private boolean softGroundPlay;

    @Getter
    @Column(name = "source_codes", nullable = false)
    private String sourceCodes;

    public RhythmRuleRow() {}
}
