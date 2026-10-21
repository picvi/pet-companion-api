package com.picvi.pet_companion_service.petlog.entity;

import com.picvi.pet_companion_service.pet.entity.Pet;
import com.picvi.pet_companion_service.petlog.domain.LogDetail;
import com.picvi.pet_companion_service.petlog.domain.LogFlag;
import com.picvi.pet_companion_service.petlog.domain.LogFood;
import com.picvi.pet_companion_service.petlog.domain.LogType;
import com.picvi.pet_companion_service.petlog.domain.LogValues;
import com.picvi.pet_companion_service.petlog.domain.LoggedAt;
import com.picvi.pet_companion_service.petlog.domain.MeasureKind;
import com.picvi.pet_companion_service.petlog.domain.Wellbeing;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "pet_log")
public class PetLog {
    @Id
    @Getter
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Getter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    @Getter
    @Column(name = "client_id", nullable = false, updatable = false)
    private UUID clientId;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private LogType type;

    @Getter
    @Enumerated(EnumType.STRING)
    private LogDetail detail;

    @Getter
    @Column(name = "local_date", nullable = false)
    private LocalDate localDate;

    @Getter
    @Column(name = "minute_of_day", nullable = false)
    private int minuteOfDay;

    @Getter
    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    @Getter
    @Column(name = "utc_offset_minutes", nullable = false)
    private int utcOffsetMinutes;

    @Getter
    @Column(name = "logged_by", updatable = false)
    private UUID loggedBy;

    @Getter
    private Boolean accident;

    @Getter
    @Enumerated(EnumType.STRING)
    private Wellbeing wellbeing;

    @Getter
    private String label;

    @Getter
    private String dose;

    @Getter
    private String note;

    @Getter
    @ElementCollection
    @CollectionTable(name = "pet_log_flag", joinColumns = @JoinColumn(name = "log_id"))
    @Column(name = "flag")
    @Enumerated(EnumType.STRING)
    @BatchSize(size = 100)
    private Set<LogFlag> flags = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "pet_log_measure", joinColumns = @JoinColumn(name = "log_id"))
    @MapKeyColumn(name = "kind")
    @MapKeyEnumerated(EnumType.STRING)
    @Column(name = "value")
    @BatchSize(size = 100)
    private Map<MeasureKind, Integer> measures = new EnumMap<>(MeasureKind.class);

    @Getter
    @ElementCollection
    @CollectionTable(name = "pet_log_food", joinColumns = @JoinColumn(name = "log_id"))
    @BatchSize(size = 100)
    private List<LoggedFood> foods = new ArrayList<>();

    @Getter
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Getter
    @Column(name = "updated_at")
    private Instant updatedAt;

    public PetLog() {}

    public PetLog(Pet pet, UUID clientId, LogType type, UUID loggedBy, LogDetail detail,
                  LogValues values, LocalDate localDate, int minuteOfDay, LoggedAt loggedAt) {
        this.pet = pet;
        this.clientId = clientId;
        this.type = type;
        this.loggedBy = loggedBy;
        this.apply(detail, values, localDate, minuteOfDay, loggedAt);
    }

    public void replace(LogDetail detail, LogValues values, LocalDate localDate, int minuteOfDay, LoggedAt loggedAt,
                        Instant updatedAt) {
        this.apply(detail, values, localDate, minuteOfDay, loggedAt);
        this.updatedAt = updatedAt;
    }

    public Integer getStoolScore() {
        return this.measures.get(MeasureKind.STOOL_SCORE);
    }

    public Integer getWeightGrams() {
        return this.measures.get(MeasureKind.WEIGHT_GRAMS);
    }

    public Integer getDurationMinutes() {
        return this.measures.get(MeasureKind.DURATION_MINUTES);
    }

    public Integer getAmountGrams() {
        return this.measures.get(MeasureKind.AMOUNT_GRAMS);
    }

    public LogValues values() {
        return new LogValues(this.accident, this.getStoolScore(), this.flags, this.getAmountGrams(), this.getWeightGrams(),
                this.getDurationMinutes(), this.wellbeing, this.label, this.dose, this.note,
                this.foods.stream().map(food -> new LogFood(food.getPetFoodId(), food.getGrams())).toList());
    }

    private void apply(LogDetail detail, LogValues values, LocalDate localDate, int minuteOfDay, LoggedAt loggedAt) {
        this.detail = detail;
        this.localDate = localDate;
        this.minuteOfDay = minuteOfDay;
        this.occurredAt = loggedAt.occurredAt();
        this.utcOffsetMinutes = loggedAt.utcOffsetMinutes();
        this.accident = values.accident();
        this.wellbeing = values.wellbeing();
        this.measures.clear();
        this.putMeasure(MeasureKind.STOOL_SCORE, values.stoolScore());
        this.putMeasure(MeasureKind.AMOUNT_GRAMS, values.amountGrams());
        this.putMeasure(MeasureKind.WEIGHT_GRAMS, values.weightGrams());
        this.putMeasure(MeasureKind.DURATION_MINUTES, values.durationMinutes());
        this.label = values.label();
        this.dose = values.dose();
        this.note = values.note();
        this.flags.clear();
        this.flags.addAll(values.flags());
        this.foods.clear();
        values.foods().forEach(food -> this.foods.add(new LoggedFood(food.petFoodId(), food.grams())));
    }

    private void putMeasure(MeasureKind kind, Integer value) {
        if (value != null) {
            this.measures.put(kind, value);
        }
    }
}
