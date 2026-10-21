package com.picvi.pet_companion_service.triage;

import com.picvi.pet_companion_service.triage.domain.FollowUpPolicy;
import com.picvi.pet_companion_service.triage.domain.ConditionEvaluator;
import com.picvi.pet_companion_service.triage.domain.TriageEngine;
import com.picvi.pet_companion_service.triage.service.RuleBookLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TriageConfig {

    @Bean
    public FollowUpPolicy followUpPolicy() {
        return new FollowUpPolicy();
    }

    @Bean
    public ConditionEvaluator conditionEvaluator() {
        return new ConditionEvaluator();
    }

    @Bean
    public TriageEngine triageEngine(ConditionEvaluator conditionEvaluator, RuleBookLoader ruleBookLoader) {
        return new TriageEngine(conditionEvaluator, ruleBookLoader.getRuleBook());
    }
}
