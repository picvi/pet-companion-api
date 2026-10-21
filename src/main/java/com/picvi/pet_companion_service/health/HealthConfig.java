package com.picvi.pet_companion_service.health;

import com.picvi.pet_companion_service.health.domain.DiaryFilter;
import com.picvi.pet_companion_service.health.domain.HealthSummaryRule;
import com.picvi.pet_companion_service.health.domain.WatchEvents;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HealthConfig {

    @Bean
    public WatchEvents watchEvents() {
        return new WatchEvents();
    }

    @Bean
    public HealthSummaryRule healthSummaryRule(WatchEvents watchEvents) {
        return new HealthSummaryRule(watchEvents);
    }

    @Bean
    public DiaryFilter diaryFilter() {
        return new DiaryFilter();
    }
}
