package com.picvi.pet_companion_service.triage.domain;

import java.util.Map;
import java.util.Set;

public record CheckFacts(String item, Set<String> flags, Map<String, String> answers, String substance,
                         Context context, FollowUpFacts followUp) {
    public CheckFacts {
        flags = Set.copyOf(flags);
        answers = Map.copyOf(answers);
    }
}
