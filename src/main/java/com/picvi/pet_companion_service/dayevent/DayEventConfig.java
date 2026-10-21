package com.picvi.pet_companion_service.dayevent;

import com.picvi.pet_companion_service.dayevent.domain.DayEventRules;
import com.picvi.pet_companion_service.dayevent.domain.EventOccurrenceExpander;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DayEventConfig {

    @Bean
    public DayEventRules dayEventRules() {
        return new DayEventRules();
    }

    @Bean
    public EventOccurrenceExpander eventOccurrenceExpander() {
        return new EventOccurrenceExpander();
    }
}
