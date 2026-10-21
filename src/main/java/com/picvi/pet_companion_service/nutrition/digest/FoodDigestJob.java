package com.picvi.pet_companion_service.nutrition.digest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/** Every morning: one email about the foods owners added since yesterday. Locked, so a second server never sends a second one. */
@Slf4j
@Component
@RequiredArgsConstructor
public class FoodDigestJob {
    private final FoodDigestService digestService;
    private final DigestMailer mailer;

    @Value("${pawstep.admin.emails:}")
    private String adminEmails;

    @Scheduled(cron = "0 0 8 * * *", zone = "Europe/Warsaw")
    @SchedulerLock(name = "foodDigest", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
    public void run() {
        List<String> recipients = Arrays.stream(this.adminEmails.split(",")).map(String::trim).filter(email -> !email.isEmpty()).toList();
        if (recipients.isEmpty() || !this.mailer.isConfigured()) {
            log.warn("Food digest skipped: set pawstep.admin.emails and spring.mail.host");
            return;
        }
        try {
            int listed = this.digestService.sendPending(recipients);
            log.info("Food digest listed {} products", listed);
        } catch (RuntimeException e) {
            log.error("Food digest failed; the products stay unlisted and the next run retries", e);
        }
    }
}
