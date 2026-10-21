package com.picvi.pet_companion_service.rhythm.domain;

import java.util.List;

public record DayPlanInput(RhythmRule rule, DaySettings settings, List<LoggedEvent> logs, List<FixedEvent> events) {

    public int awakeMinutes() {
        Integer custom = this.settings.customAwakeMinutes();
        return custom != null ? custom : this.rule.awakeMinutes().standard();
    }

    public int bedMinute() {
        return this.settings.bedMinute();
    }
}
