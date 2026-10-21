package com.picvi.pet_companion_service.breeds.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.math.BigInteger;
import java.time.Instant;

@Entity
@Table(name = "breeds")
public class Breed {
    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private BigInteger id;

    @Getter
    @Column(unique = true)
    private String code;

    @Column(unique = true)
    private String name;

    private String description;

    @Column(name = "recognition_status")
    private String recognitionStatus;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(name = "size_category", nullable = false)
    private SizeCategory sizeCategory;

    @Getter
    @Column
    private String variety;

    @Column(name = "created_at")
    @CreatedDate
    private Instant createdAt;

    @Column(name = "updated_at")
    @LastModifiedDate
    private Instant updatedAt;

    public Breed() {}
}
