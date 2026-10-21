package com.picvi.pet_companion_service.exception.development;

import com.picvi.pet_companion_service.exception.ApiException;

public class SessionNotFound extends ApiException {
    public SessionNotFound() {
        super("Session not found", "SESSION_NOT_FOUND");
    }
}
