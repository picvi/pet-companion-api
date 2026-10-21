package com.picvi.pet_companion_service.pet;

import com.picvi.pet_companion_service.pet.domain.VaccinationPrompt;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PetConfig {

    @Bean
    public VaccinationPrompt vaccinationPrompt() {
        return new VaccinationPrompt();
    }
}
