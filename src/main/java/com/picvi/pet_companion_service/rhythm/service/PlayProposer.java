package com.picvi.pet_companion_service.rhythm.service;

import com.picvi.pet_companion_service.development.domain.ActivityKind;
import com.picvi.pet_companion_service.development.dto.ActivityRefDto;
import com.picvi.pet_companion_service.development.dto.PlanItemDto;
import com.picvi.pet_companion_service.development.dto.TodayResponse;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Component
public class PlayProposer {

    public Optional<ActivityRefDto> propose(TodayResponse today) {
        Stream<ActivityRefDto> fromFocus = today.focus() != null && !today.focus().doneToday()
                ? Stream.of(today.focus().activity())
                : Stream.empty();
        Stream<ActivityRefDto> fromPlan = today.plan().stream()
                .filter(item -> !item.doneToday())
                .map(PlanItemDto::activity);

        return Stream.concat(fromFocus, fromPlan)
                .filter(Objects::nonNull)
                .filter(activity -> activity.kind() == ActivityKind.SKILL)
                .filter(activity -> !activity.aloneTracker())
                .findFirst();
    }
}
