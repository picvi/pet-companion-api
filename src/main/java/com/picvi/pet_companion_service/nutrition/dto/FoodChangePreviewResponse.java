package com.picvi.pet_companion_service.nutrition.dto;

import com.picvi.pet_companion_service.nutrition.domain.AllergyMatch;
import com.picvi.pet_companion_service.nutrition.domain.TransitionPace;

import java.time.LocalDate;
import java.util.List;

/** What a change would look like, before it starts. {@code early} is true when the dog has been home under two weeks. */
public record FoodChangePreviewResponse(TransitionPace pace,
                                        LocalDate startOn,
                                        LocalDate endOn,
                                        int totalDays,
                                        boolean early,
                                        List<PlanStepDto> steps,
                                        List<AllergyMatch> allergyMatches) {
}
