package com.picvi.pet_companion_service.medical;

import com.picvi.pet_companion_service.medical.domain.VetAppointmentRule;
import com.picvi.pet_companion_service.medical.domain.VetVisitRules;
import com.picvi.pet_companion_service.medical.domain.VisitHistoryPaging;
import com.picvi.pet_companion_service.medical.domain.VisitOutcomeRule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MedicalConfig {

    @Bean
    public VetVisitRules vetVisitRules() {
        return new VetVisitRules();
    }

    @Bean
    public VetAppointmentRule vetAppointmentRule() {
        return new VetAppointmentRule();
    }

    @Bean
    public VisitOutcomeRule visitOutcomeRule() {
        return new VisitOutcomeRule();
    }

    @Bean
    public VisitHistoryPaging visitHistoryPaging() {
        return new VisitHistoryPaging();
    }
}
