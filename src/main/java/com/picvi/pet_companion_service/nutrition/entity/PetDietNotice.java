package com.picvi.pet_companion_service.nutrition.entity;

import com.picvi.pet_companion_service.nutrition.domain.NoticeCode;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "pet_diet_notice")
@IdClass(PetDietNoticeId.class)
public class PetDietNotice {
    @Id
    @Getter
    @Column(name = "pet_id")
    private UUID petId;

    @Id
    @Column(name = "code")
    private String code;

    @Getter
    @Column(name = "seen_on", nullable = false)
    private LocalDate seenOn;

    @Getter
    @Column(name = "seen_at", nullable = false)
    private OffsetDateTime seenAt;

    public PetDietNotice() {}

    public PetDietNotice(UUID petId, NoticeCode code, LocalDate seenOn, OffsetDateTime seenAt) {
        this.petId = petId;
        this.code = code.name();
        this.seenOn = seenOn;
        this.seenAt = seenAt;
    }

    public NoticeCode getCode() {
        return NoticeCode.valueOf(this.code);
    }
}
