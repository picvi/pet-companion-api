package com.picvi.pet_companion_service.exception.nutrition;

import com.picvi.pet_companion_service.exception.ApiException;
import com.picvi.pet_companion_service.nutrition.domain.AllergyMatch;
import lombok.Getter;

import java.util.List;

/** The food touches one of the pet's allergens; it is saved only when the owner confirms. */
public class AllergyMatchRequired extends ApiException {
    @Getter
    private final List<AllergyMatch> matches;

    public AllergyMatchRequired(List<AllergyMatch> matches) {
        super("This food contains something the dog is allergic to. Confirm to save it anyway", "ALLERGY_MATCH");
        this.matches = matches;
    }
}
