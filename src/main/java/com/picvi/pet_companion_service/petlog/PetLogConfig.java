package com.picvi.pet_companion_service.petlog;

import com.picvi.pet_companion_service.petlog.domain.OccurredAtCalculator;
import com.picvi.pet_companion_service.petlog.domain.PetLogRules;
import com.picvi.pet_companion_service.petlog.domain.RecentLogs;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;

@Configuration
public class PetLogConfig {

    @Bean
    public PetLogRules petLogRules() {
        return new PetLogRules();
    }

    @Bean
    public RecentLogs recentLogs() {
        return new RecentLogs();
    }

    @Bean
    public OccurredAtCalculator occurredAtCalculator() {
        return new OccurredAtCalculator(ZoneId.of("Europe/Warsaw"));
    }
}
