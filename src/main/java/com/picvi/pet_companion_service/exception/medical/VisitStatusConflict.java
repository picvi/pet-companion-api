package com.picvi.pet_companion_service.exception.medical;

import com.picvi.pet_companion_service.exception.ApiException;

public class VisitStatusConflict extends ApiException {
    public VisitStatusConflict() {
        super("This visit is not in a state that allows that", "VISIT_STATUS_CONFLICT");
    }
}
