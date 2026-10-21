package com.picvi.pet_companion_service.care.repository;

import com.picvi.pet_companion_service.care.entity.CareItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CareItemRepository extends JpaRepository<CareItem, String> {

    List<CareItem> findAllByActiveTrueOrderBySortOrder();

    Optional<CareItem> findByCodeAndActiveTrue(String code);
}
