package com.picvi.pet_companion_service.common;

import com.picvi.pet_companion_service.exception.InvalidDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class ClientDateValidator {
    private final Clock clock;

    public void validate(LocalDate date) {
        LocalDate serverToday = LocalDate.now(clock);
        if (date == null || date.isBefore(serverToday.minusDays(1)) || date.isAfter(serverToday.plusDays(1))) {
            throw new InvalidDate();
        }
    }
}
