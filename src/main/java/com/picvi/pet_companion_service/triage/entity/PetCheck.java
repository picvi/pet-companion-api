package com.picvi.pet_companion_service.triage.entity;

import com.picvi.pet_companion_service.triage.domain.CheckSource;
import com.picvi.pet_companion_service.triage.domain.CheckStatus;
import com.picvi.pet_companion_service.triage.domain.Level;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "pet_check")
public class PetCheck {
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "pet_id", nullable = false, updatable = false)
    private UUID petId;

    @Column(name = "client_id", nullable = false, updatable = false)
    private UUID clientId;

    @Column(name = "occurred_on", nullable = false)
    private LocalDate occurredOn;

    @Column(name = "minute_of_day", nullable = false)
    private int minuteOfDay;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private CheckSource source;

    @Column(nullable = false, updatable = false)
    private String item;

    @Column(nullable = false, updatable = false)
    private String category;

    @Convert(converter = CheckFlagsConverter.class)
    @Column(nullable = false)
    private Set<String> flags = Set.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, String> answers = Map.of();

    private String substance;

    @Column(length = 500)
    private String note;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> context;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Level level;

    @Column(name = "rule_id", nullable = false)
    private String ruleId;

    @Column(nullable = false)
    private String rulebook;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> result;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CheckStatus status;

    @Column(name = "check_back_due_at")
    private Instant checkBackDueAt;

    @Column(name = "resolved_on")
    private LocalDate resolvedOn;

    @Version
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PetCheck() {
    }

    public PetCheck(UUID petId, UUID clientId, LocalDate occurredOn, int minuteOfDay, CheckSource source, String item,
                    String category, Set<String> flags, Map<String, String> answers, String substance, String note,
                    Map<String, Object> context, Level level, String ruleId, String rulebook, Map<String, Object> result,
                    CheckStatus status, Instant checkBackDueAt, Instant now) {
        this.petId = petId;
        this.clientId = clientId;
        this.occurredOn = occurredOn;
        this.minuteOfDay = minuteOfDay;
        this.source = source;
        this.item = item;
        this.category = category;
        this.flags = Set.copyOf(flags);
        this.answers = Collections.unmodifiableMap(new LinkedHashMap<>(answers));
        this.substance = substance;
        this.note = note;
        this.context = Collections.unmodifiableMap(new LinkedHashMap<>(context));
        this.level = level;
        this.ruleId = ruleId;
        this.rulebook = rulebook;
        this.result = Collections.unmodifiableMap(new LinkedHashMap<>(result));
        this.status = status;
        this.checkBackDueAt = checkBackDueAt;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void updateFollowUp(Level level, CheckStatus status, Map<String, Object> result, Instant checkBackDueAt,
                               LocalDate resolvedOn, Instant now) {
        this.level = level;
        this.status = status;
        this.result = Collections.unmodifiableMap(new LinkedHashMap<>(result));
        this.checkBackDueAt = checkBackDueAt;
        this.resolvedOn = resolvedOn;
        this.updatedAt = now;
    }

    public UUID getId() { return this.id; }
    public UUID getPetId() { return this.petId; }
    public UUID getClientId() { return this.clientId; }
    public LocalDate getOccurredOn() { return this.occurredOn; }
    public int getMinuteOfDay() { return this.minuteOfDay; }
    public CheckSource getSource() { return this.source; }
    public String getItem() { return this.item; }
    public String getCategory() { return this.category; }
    public Set<String> getFlags() { return this.flags; }
    public Map<String, String> getAnswers() { return this.answers; }
    public String getSubstance() { return this.substance; }
    public String getNote() { return this.note; }
    public Map<String, Object> getContext() { return this.context; }
    public Level getLevel() { return this.level; }
    public String getRuleId() { return this.ruleId; }
    public String getRulebook() { return this.rulebook; }
    public Map<String, Object> getResult() { return this.result; }
    public CheckStatus getStatus() { return this.status; }
    public Instant getCheckBackDueAt() { return this.checkBackDueAt; }
    public LocalDate getResolvedOn() { return this.resolvedOn; }
    public long getVersion() { return this.version; }
    public Instant getCreatedAt() { return this.createdAt; }
    public Instant getUpdatedAt() { return this.updatedAt; }
}
