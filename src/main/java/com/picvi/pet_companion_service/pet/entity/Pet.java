package com.picvi.pet_companion_service.pet.entity;

import com.picvi.pet_companion_service.breeds.entity.Breed;
import com.picvi.pet_companion_service.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.util.UUID;
import java.time.Instant;

@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "pets")
public class Pet {
    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Setter
    @Getter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Setter
    @Getter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "breed_id", nullable = false)
    private Breed breed;

    @Setter
    @Getter
    @Column(nullable = false)
    private String name;

    @Setter
    @Getter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Sex sex;

    @Setter
    @Getter
    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Setter
    @Getter
    @Column
    private Boolean neutered;

    @Setter
    @Getter
    @Column(name = "microchip_number")
    private String microchipNumber;

    @Setter
    @Getter
    @Enumerated(EnumType.STRING)
    @Column(name = "vaccination_status", nullable = false)
    private VaccinationStatus vaccinationStatus = VaccinationStatus.UNKNOWN;

    @Setter
    @Getter
    @Column(name = "vaccination_status_changed_on")
    private LocalDate vaccinationStatusChangedOn;

    @Setter
    @Getter
    @Column(name = "vaccination_prompt_count", nullable = false)
    private int vaccinationPromptCount;

    @Getter
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Pet() {}
}
