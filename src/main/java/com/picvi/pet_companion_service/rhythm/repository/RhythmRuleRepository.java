package com.picvi.pet_companion_service.rhythm.repository;

import com.picvi.pet_companion_service.rhythm.entity.RhythmRuleRow;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RhythmRuleRepository extends JpaRepository<RhythmRuleRow, Long> {
}
