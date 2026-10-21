package com.picvi.pet_companion_service.development.service;

import com.picvi.pet_companion_service.development.domain.ActivityDefinition;
import com.picvi.pet_companion_service.development.domain.ProgressState;
import com.picvi.pet_companion_service.development.entity.DevelopmentActivity;
import com.picvi.pet_companion_service.development.entity.PetActivityProgress;
import com.picvi.pet_companion_service.development.repository.DevelopmentActivityRepository;
import com.picvi.pet_companion_service.development.repository.PetActivityProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ActivityDataLoader {
    private final DevelopmentActivityRepository activityRepository;
    private final PetActivityProgressRepository progressRepository;

    public List<ActivityDefinition> loadActiveActivities() {
        return this.activityRepository.findAllByActiveTrue().stream()
                .map(DevelopmentActivity::toDefinition)
                .toList();
    }

    public Map<String, ProgressState> loadProgress(UUID petId) {
        return this.progressRepository.findAllByPetId(petId).stream()
                .collect(Collectors.toMap(row -> row.getActivity().getCode(), PetActivityProgress::toState));
    }
}
