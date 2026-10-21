package com.picvi.pet_companion_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class PetCompanionServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PetCompanionServiceApplication.class, args);
	}

}
