package com.picvi.pet_companion_service.exception.care;

import com.picvi.pet_companion_service.exception.ApiException;

public class CareItemNotFound extends ApiException {
    public CareItemNotFound() {
        super("Care item not found", "CARE_ITEM_NOT_FOUND");
    }
}
