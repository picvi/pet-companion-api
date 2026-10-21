package com.picvi.pet_companion_service.triage.domain;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record Rule(String id, Set<String> items, Condition when, Level level, int priority, String reason,
                   Map<String, FactName> reasonParams, List<RuleStep> steps, List<String> changeIf,
                   List<RuleAction> actions, Integer checkBackMinutes, String briefConcern,
                   List<String> caseIds, List<String> sources, RuleReview review, boolean redFlag) {
    public Rule {
        items = Set.copyOf(items);
        reasonParams = Map.copyOf(reasonParams);
        steps = List.copyOf(steps);
        changeIf = List.copyOf(changeIf);
        actions = List.copyOf(actions);
        caseIds = List.copyOf(caseIds);
        sources = List.copyOf(sources);
    }

    public record RuleReview(String status, String reviewer, String reviewedOn) {
    }

    public record RuleStep(String code, Map<String, FactName> params) {
        public RuleStep { params = Map.copyOf(params); }
    }

    public record RuleAction(String code, Map<String, String> params) {
        public RuleAction { params = Map.copyOf(params); }
    }
}
