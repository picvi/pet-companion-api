package com.picvi.pet_companion_service.exception.care;

import com.picvi.pet_companion_service.exception.ApiException;

public class IntervalNotEditable extends ApiException {
    public IntervalNotEditable() {
        super("This item does not allow a custom interval", "INTERVAL_NOT_EDITABLE");
    }
}
