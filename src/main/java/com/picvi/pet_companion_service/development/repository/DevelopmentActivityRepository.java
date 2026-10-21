package com.picvi.pet_companion_service.development.repository;

import com.picvi.pet_companion_service.development.entity.DevelopmentActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DevelopmentActivityRepository extends JpaRepository<DevelopmentActivity, Long> {

    List<DevelopmentActivity> findAllByActiveTrue();

    Optional<DevelopmentActivity> findByCodeAndActiveTrue(String code);
}
