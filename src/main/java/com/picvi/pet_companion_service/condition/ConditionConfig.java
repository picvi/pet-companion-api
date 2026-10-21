package com.picvi.pet_companion_service.condition;

import com.picvi.pet_companion_service.condition.domain.ConditionRules;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConditionConfig {

    @Bean
    public ConditionRules conditionRules() {
        return new ConditionRules();
    }
}
