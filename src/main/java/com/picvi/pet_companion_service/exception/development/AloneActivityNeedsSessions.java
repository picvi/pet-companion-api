package com.picvi.pet_companion_service.exception.development;

import com.picvi.pet_companion_service.exception.ApiException;

public class AloneActivityNeedsSessions extends ApiException {
    public AloneActivityNeedsSessions() {
        super("Alone-time activities are logged as sessions", "ALONE_ACTIVITY_NEEDS_SESSIONS");
    }
}
