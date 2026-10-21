package com.picvi.pet_companion_service.care.repository;

import com.picvi.pet_companion_service.care.entity.CareProtocolStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CareProtocolStepRepository extends JpaRepository<CareProtocolStep, Long> {

    List<CareProtocolStep> findAllByOrderByItemCodeAscStepOrderAsc();

    List<CareProtocolStep> findAllByItemCodeOrderByStepOrderAsc(String itemCode);
}
