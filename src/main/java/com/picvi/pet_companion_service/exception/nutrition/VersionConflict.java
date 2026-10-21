package com.picvi.pet_companion_service.exception.nutrition;

import com.picvi.pet_companion_service.exception.ApiException;

public class VersionConflict extends ApiException {
    public VersionConflict() {
        super("This was changed elsewhere. Reload and try again", "VERSION_CONFLICT");
    }
}
