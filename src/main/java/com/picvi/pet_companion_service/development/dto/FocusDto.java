package com.picvi.pet_companion_service.development.dto;

import com.picvi.pet_companion_service.development.domain.Focus;
import com.picvi.pet_companion_service.development.domain.FocusReason;
import com.picvi.pet_companion_service.development.domain.FocusType;

public record FocusDto(FocusType type, ActivityRefDto activity, FocusReason reason, boolean doneToday) {

    public static FocusDto of(Focus focus) {
        return new FocusDto(focus.type(), ActivityRefDto.of(focus.activity()), focus.reason(), focus.doneToday());
    }
}
