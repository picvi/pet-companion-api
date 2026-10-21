package com.picvi.pet_companion_service.triage.domain;

import java.util.List;

public record RuleBook(String version, List<Rule> rules) {
    public RuleBook {
        rules = List.copyOf(rules);
    }

    public List<Rule> rulesFor(String item) {
        return this.rules.stream().filter(rule -> rule.items().contains(item) || rule.items().contains("ANY")).toList();
    }
}
