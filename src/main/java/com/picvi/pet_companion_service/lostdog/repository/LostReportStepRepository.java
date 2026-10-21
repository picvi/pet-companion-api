package com.picvi.pet_companion_service.lostdog.repository;

import com.picvi.pet_companion_service.lostdog.entity.LostReportStep;
import com.picvi.pet_companion_service.lostdog.entity.LostReportStepId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LostReportStepRepository extends JpaRepository<LostReportStep, LostReportStepId> {
    List<LostReportStep> findAllByIdReportId(UUID reportId);
}
