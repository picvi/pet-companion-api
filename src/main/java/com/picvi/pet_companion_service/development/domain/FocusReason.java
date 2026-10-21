package com.picvi.pet_companion_service.development.domain;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FocusReason(FocusReasonCode code, Integer ageWeeks, Integer weeksLeftInWindow,
                          Integer categoryCompleted, Integer categoryTotal, Integer tipIndex) {

    public static FocusReason ofStage(FocusReasonCode code, StageInfo stage) {
        return new FocusReason(code, stage.ageWeeks(), stage.weeksLeftInWindow(), null, null, null);
    }

    public static FocusReason ofCategory(FocusReasonCode code, StageInfo stage, int categoryCompleted, int categoryTotal) {
        return new FocusReason(code, stage.ageWeeks(), stage.weeksLeftInWindow(), categoryCompleted, categoryTotal, null);
    }

    public static FocusReason stageTip(int tipIndex) {
        return new FocusReason(FocusReasonCode.STAGE_TIP, null, null, null, null, tipIndex);
    }
}
