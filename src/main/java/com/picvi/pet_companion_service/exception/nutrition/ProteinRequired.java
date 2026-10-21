package com.picvi.pet_companion_service.exception.nutrition;

import com.picvi.pet_companion_service.exception.ApiException;

public class ProteinRequired extends ApiException {
    public ProteinRequired() {
        super("Choose the main protein, or say you are not sure", "PROTEIN_REQUIRED");
    }
}
