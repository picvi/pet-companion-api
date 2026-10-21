package com.picvi.pet_companion_service.lostdog.repository;

import com.picvi.pet_companion_service.lostdog.entity.PetLostProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PetLostProfileRepository extends JpaRepository<PetLostProfile, UUID> {}
