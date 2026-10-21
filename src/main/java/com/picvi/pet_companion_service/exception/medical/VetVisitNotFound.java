package com.picvi.pet_companion_service.exception.medical;

import com.picvi.pet_companion_service.exception.ApiException;

public class VetVisitNotFound extends ApiException {
    public VetVisitNotFound() {
        super("This vet visit was not found", "VET_VISIT_NOT_FOUND");
    }
}
