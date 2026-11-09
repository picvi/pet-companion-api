package com.picvi.pet_companion_service.petlog.api;

import com.picvi.pet_companion_service.petlog.domain.CourseDose;

import java.util.UUID;

/** Asked before a medicine log is linked to a course; the medication module answers it. */
public interface CourseDoseCheck {
    void verify(UUID petId, CourseDose dose);
}
