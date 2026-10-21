package com.picvi.pet_companion_service.triage.domain;

import java.util.Comparator;
import java.util.List;

public class TriageEngine {
    private final ConditionEvaluator conditionEvaluator;
    private final RuleBook ruleBook;

    public TriageEngine(ConditionEvaluator conditionEvaluator, RuleBook ruleBook) {
        this.conditionEvaluator = conditionEvaluator;
        this.ruleBook = ruleBook;
    }

    public Evaluation evaluate(CheckFacts facts) {
        return this.evaluate(facts, this.ruleBook);
    }

    public Evaluation evaluate(CheckFacts facts, RuleBook book) {
        List<Rule> matched = book.rulesFor(facts.item()).stream()
                .filter(rule -> this.conditionEvaluator.matches(rule.when(), facts))
                .toList();
        Rule winner = matched.stream()
                .max(Comparator.comparing(Rule::level)
                        .thenComparingInt(Rule::priority)
                        .thenComparing(Rule::id, Comparator.reverseOrder()))
                .orElseThrow(() -> new IllegalStateException("No triage rule matched for " + facts.item()));
        return new Evaluation(winner);
    }
}
