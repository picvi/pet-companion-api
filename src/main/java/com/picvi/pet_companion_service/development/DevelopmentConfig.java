package com.picvi.pet_companion_service.development;

import com.picvi.pet_companion_service.development.domain.ActivityRanking;
import com.picvi.pet_companion_service.development.domain.LifeStageCalculator;
import com.picvi.pet_companion_service.development.domain.ProgressRules;
import com.picvi.pet_companion_service.development.domain.StageProgressCalculator;
import com.picvi.pet_companion_service.development.domain.TodayPlanner;
import com.picvi.pet_companion_service.pet.domain.VaccinationPrompt;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DevelopmentConfig {

    @Bean
    public LifeStageCalculator lifeStageCalculator() {
        return new LifeStageCalculator();
    }

    @Bean
    public ProgressRules progressRules() {
        return new ProgressRules();
    }

    @Bean
    public StageProgressCalculator stageProgressCalculator() {
        return new StageProgressCalculator();
    }

    @Bean
    public ActivityRanking activityRanking() {
        return new ActivityRanking();
    }

    @Bean
    public TodayPlanner todayPlanner(LifeStageCalculator lifeStageCalculator,
                                     StageProgressCalculator stageProgressCalculator,
                                     VaccinationPrompt vaccinationPrompt) {
        return new TodayPlanner(lifeStageCalculator, stageProgressCalculator, vaccinationPrompt);
    }
}
