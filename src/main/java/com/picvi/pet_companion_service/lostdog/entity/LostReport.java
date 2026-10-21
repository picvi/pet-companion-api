package com.picvi.pet_companion_service.lostdog.entity;

import com.picvi.pet_companion_service.lostdog.domain.LostStatus;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lost_report")
public class LostReport {
    @Id @Getter
    @GeneratedValue @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Getter
    @Column(name = "pet_id", nullable = false, updatable = false)
    private UUID petId;

    @Getter
    @Column(name = "client_id", nullable = false, updatable = false)
    private UUID clientId;

    @Getter
    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Getter
    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Getter
    @Column(name = "last_seen_place", nullable = false, length = 300)
    private String lastSeenPlace;

    @Getter
    @Column(name = "last_seen_lat")
    private Double lastSeenLat;

    @Getter
    @Column(name = "last_seen_lng")
    private Double lastSeenLng;

    @Getter
    @Column(name = "poster_phone", length = 32)
    private String posterPhone;

    @Getter
    @Column(name = "poster_message", length = 300)
    private String posterMessage;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private LostStatus status;

    @Getter
    @Column(name = "ended_at")
    private Instant endedAt;

    @Getter @Version
    private long version;

    @Getter
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Getter
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected LostReport() {}

    public LostReport(UUID petId, UUID clientId, Instant now, Instant lastSeenAt, String place,
                      Double lat, Double lng, String phone) {
        this.petId = petId;
        this.clientId = clientId;
        this.startedAt = now;
        this.createdAt = now;
        this.updatedAt = now;
        this.status = LostStatus.ACTIVE;
        this.updateLastSeen(lastSeenAt, place, lat, lng, phone, null, now);
    }

    public void updateLastSeen(Instant at, String place, Double lat, Double lng, String phone,
                               String posterMessage, Instant now) {
        this.lastSeenAt = at;
        this.lastSeenPlace = place;
        this.lastSeenLat = lat;
        this.lastSeenLng = lng;
        this.posterPhone = phone;
        this.posterMessage = posterMessage;
        this.updatedAt = now;
    }

    public void close(LostStatus finalStatus, Instant now) {
        if (this.status != LostStatus.ACTIVE || finalStatus == LostStatus.ACTIVE) {
            throw new IllegalStateException("Only an active lost report can be closed");
        }
        this.status = finalStatus;
        this.endedAt = now;
        this.updatedAt = now;
    }

    public void touch(Instant now) {
        this.updatedAt = now;
    }
}
