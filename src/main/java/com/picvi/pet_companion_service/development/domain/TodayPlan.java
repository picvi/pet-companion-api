package com.picvi.pet_companion_service.development.domain;

import java.util.List;

public record TodayPlan(StageInfo stage, StageProgress stageProgress, Focus focus, List<PlanItem> plan,
                        boolean showWorriedCard, boolean vaccinationPromptDue) {
}
