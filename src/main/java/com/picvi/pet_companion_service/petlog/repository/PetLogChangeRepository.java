package com.picvi.pet_companion_service.petlog.repository;

import com.picvi.pet_companion_service.petlog.entity.PetLogChangeRow;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PetLogChangeRepository extends JpaRepository<PetLogChangeRow, Long> {
}
