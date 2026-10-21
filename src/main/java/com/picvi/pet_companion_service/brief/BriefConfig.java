package com.picvi.pet_companion_service.brief;

import com.picvi.pet_companion_service.brief.domain.ConcernCounter;
import com.picvi.pet_companion_service.brief.domain.QuestionSuggester;
import com.picvi.pet_companion_service.brief.domain.TrendCalculator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BriefConfig {

    @Bean
    public ConcernCounter concernCounter() {
        return new ConcernCounter();
    }

    @Bean
    public TrendCalculator trendCalculator() {
        return new TrendCalculator();
    }

    @Bean
    public QuestionSuggester questionSuggester() {
        return new QuestionSuggester();
    }
}
