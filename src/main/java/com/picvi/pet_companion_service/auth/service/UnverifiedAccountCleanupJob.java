package com.picvi.pet_companion_service.auth.service;

import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** Frees usernames and emails held by sign-ups that never confirmed their address. */
@Component
@RequiredArgsConstructor
public class UnverifiedAccountCleanupJob {
    private static final Duration MAX_AGE = Duration.ofDays(7);

    private final EmailVerificationService emailVerificationService;
    private final PasswordResetService passwordResetService;

    @Scheduled(cron = "0 30 3 * * *", zone = "Europe/Warsaw")
    @SchedulerLock(name = "unverifiedAccountCleanup", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
    public void run() {
        this.emailVerificationService.purgeStale(MAX_AGE);
        this.passwordResetService.purgeExpired();
    }
}
