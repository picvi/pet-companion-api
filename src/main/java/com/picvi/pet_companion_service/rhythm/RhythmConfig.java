package com.picvi.pet_companion_service.rhythm;

import com.picvi.pet_companion_service.rhythm.domain.DayPlanner;
import com.picvi.pet_companion_service.rhythm.domain.DefaultDayProposer;
import com.picvi.pet_companion_service.rhythm.domain.RhythmNotices;
import com.picvi.pet_companion_service.rhythm.domain.RhythmRuleResolver;
import com.picvi.pet_companion_service.rhythm.domain.RhythmSettingsValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RhythmConfig {

    @Bean
    public RhythmRuleResolver rhythmRuleResolver() {
        return new RhythmRuleResolver();
    }

    @Bean
    public DayPlanner dayPlanner() {
        return new DayPlanner();
    }

    @Bean
    public DefaultDayProposer defaultDayProposer() {
        return new DefaultDayProposer();
    }

    @Bean
    public RhythmNotices rhythmNotices() {
        return new RhythmNotices();
    }

    @Bean
    public RhythmSettingsValidator rhythmSettingsValidator() {
        return new RhythmSettingsValidator();
    }
}
