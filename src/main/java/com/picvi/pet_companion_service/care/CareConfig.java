package com.picvi.pet_companion_service.care;

import com.picvi.pet_companion_service.care.domain.CareScheduleCalculator;
import com.picvi.pet_companion_service.care.domain.VaccinationStatusRule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CareConfig {

    @Bean
    public CareScheduleCalculator careScheduleCalculator() {
        return new CareScheduleCalculator();
    }

    @Bean
    public VaccinationStatusRule vaccinationStatusRule() {
        return new VaccinationStatusRule();
    }
}
