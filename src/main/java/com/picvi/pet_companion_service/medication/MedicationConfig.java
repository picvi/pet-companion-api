package com.picvi.pet_companion_service.medication;

import com.picvi.pet_companion_service.medication.domain.CourseRules;
import com.picvi.pet_companion_service.medication.domain.CourseStatusRule;
import com.picvi.pet_companion_service.medication.domain.DoseTimesCalculator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MedicationConfig {

    @Bean
    public DoseTimesCalculator doseTimesCalculator() {
        return new DoseTimesCalculator();
    }

    @Bean
    public CourseRules courseRules() {
        return new CourseRules();
    }

    @Bean
    public CourseStatusRule courseStatusRule() {
        return new CourseStatusRule();
    }
}
