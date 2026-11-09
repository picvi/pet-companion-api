package com.picvi.pet_companion_service.exception.dayevent;

import com.picvi.pet_companion_service.exception.ApiException;

public class UseVetPlan extends ApiException {
    public UseVetPlan() {
        super("A vet visit is booked by planning a visit", "USE_VET_PLAN");
    }
}
