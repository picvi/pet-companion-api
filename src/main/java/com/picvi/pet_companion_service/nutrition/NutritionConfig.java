package com.picvi.pet_companion_service.nutrition;

import com.picvi.pet_companion_service.nutrition.digest.FoodDigestFormatter;
import com.picvi.pet_companion_service.nutrition.domain.AllergyMatcher;
import com.picvi.pet_companion_service.nutrition.domain.FoodAskPolicy;
import com.picvi.pet_companion_service.nutrition.domain.FoodProductRules;
import com.picvi.pet_companion_service.nutrition.domain.MealPortionCalculator;
import com.picvi.pet_companion_service.nutrition.domain.PetFoodRules;
import com.picvi.pet_companion_service.nutrition.domain.ProteinExposureCalculator;
import com.picvi.pet_companion_service.nutrition.domain.TransitionPlanCalculator;
import com.picvi.pet_companion_service.nutrition.domain.TummyReactionPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NutritionConfig {

    @Bean
    public FoodProductRules foodProductRules() {
        return new FoodProductRules();
    }

    @Bean
    public MealPortionCalculator mealPortionCalculator() {
        return new MealPortionCalculator();
    }

    @Bean
    public PetFoodRules petFoodRules() {
        return new PetFoodRules();
    }

    @Bean
    public FoodAskPolicy foodAskPolicy() {
        return new FoodAskPolicy();
    }

    @Bean
    public AllergyMatcher allergyMatcher() {
        return new AllergyMatcher();
    }

    @Bean
    public ProteinExposureCalculator proteinExposureCalculator() {
        return new ProteinExposureCalculator();
    }

    @Bean
    public TransitionPlanCalculator transitionPlanCalculator() {
        return new TransitionPlanCalculator();
    }

    @Bean
    public TummyReactionPolicy tummyReactionPolicy() {
        return new TummyReactionPolicy();
    }

    @Bean
    public FoodDigestFormatter foodDigestFormatter() {
        return new FoodDigestFormatter();
    }
}
