package com.picvi.pet_companion_service.lostdog.exception;

import com.picvi.pet_companion_service.exception.ApiException;

public class LostReportNotFound extends ApiException {
    public LostReportNotFound() {
        super("This lost report was not found", "LOST_REPORT_NOT_FOUND");
    }
}
