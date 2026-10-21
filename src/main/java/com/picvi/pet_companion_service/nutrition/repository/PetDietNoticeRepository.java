package com.picvi.pet_companion_service.nutrition.repository;

import com.picvi.pet_companion_service.nutrition.entity.PetDietNotice;
import com.picvi.pet_companion_service.nutrition.entity.PetDietNoticeId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PetDietNoticeRepository extends JpaRepository<PetDietNotice, PetDietNoticeId> {

    List<PetDietNotice> findAllByPetId(UUID petId);
}
