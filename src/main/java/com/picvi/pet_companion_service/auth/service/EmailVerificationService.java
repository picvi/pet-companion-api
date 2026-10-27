package com.picvi.pet_companion_service.auth.service;

import com.picvi.pet_companion_service.auth.entity.EmailVerificationCode;
import com.picvi.pet_companion_service.auth.repository.EmailVerificationCodeRepository;
import com.picvi.pet_companion_service.exception.InvalidVerificationCode;
import com.picvi.pet_companion_service.user.entity.User;
import com.picvi.pet_companion_service.user.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {
    static final int MAX_ATTEMPTS = 5;
    private static final Duration CODE_TTL = Duration.ofMinutes(15);
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    private static final Duration SEND_WINDOW = Duration.ofHours(1);
    static final int MAX_SENDS_PER_WINDOW = 5;

    private final EmailVerificationCodeRepository codeRepository;
    private final UsersRepository usersRepository;
    private final VerificationMailer mailer;
    private final SecureRandom secureRandom = new SecureRandom();

    /** Key for the code hashes, so a leaked table cannot be brute-forced offline without it. */
    @Value("${spring.jwt.secret}")
    private String hashSecret;

    /** Whether a new code may be issued now: not within the cooldown, and not past the hourly cap. */
    @Transactional
    public boolean canSend(User user) {
        return this.codeRepository.findByUserId(user.getId()).map(this::canSend).orElse(true);
    }

    /**
     * Replaces any earlier code and mails a new one once the surrounding transaction has committed.
     * Returns false, doing nothing, when the cooldown or the hourly cap says no; guesses cannot be refreshed by asking again.
     */
    @Transactional
    public boolean sendVerification(User user) {
        Instant now = Instant.now();
        String code = String.format("%06d", this.secureRandom.nextInt(1_000_000));
        String hash = this.hash(user.getId(), code);
        Instant expiresAt = now.plus(CODE_TTL);
        EmailVerificationCode existing = this.codeRepository.findByUserId(user.getId()).orElse(null);
        if (existing == null) {
            this.codeRepository.save(new EmailVerificationCode(user, hash, expiresAt));
        } else if (this.canSend(existing)) {
            existing.reissue(hash, expiresAt, now, SEND_WINDOW);
        } else {
            return false;
        }
        this.afterCommit(() -> this.mailSafely(user.getEmail(), code));
        return true;
    }

    /**
     * Every failure looks the same to the caller. A wrong guess is counted and kept despite the exception;
     * after MAX_ATTEMPTS the code is dead and a new one has to be requested.
     */
    @Transactional(noRollbackFor = InvalidVerificationCode.class)
    public User verify(String email, String code) {
        User user = this.usersRepository.findByEmail(EmailAddress.normalise(email)).orElse(null);
        if (user == null || user.isEmailVerified()) {
            throw new InvalidVerificationCode();
        }
        EmailVerificationCode stored = this.codeRepository.findByUserId(user.getId())
                .orElseThrow(InvalidVerificationCode::new);
        if (stored.getExpiresAt().isBefore(Instant.now()) || stored.getAttempts() >= MAX_ATTEMPTS) {
            throw new InvalidVerificationCode();
        }
        if (!MessageDigest.isEqual(
                stored.getCodeHash().getBytes(StandardCharsets.UTF_8),
                this.hash(user.getId(), code).getBytes(StandardCharsets.UTF_8))) {
            stored.recordWrongAttempt();
            throw new InvalidVerificationCode();
        }
        user.setEmailVerified(true);
        this.codeRepository.deleteAllByUserId(user.getId());
        return user;
    }

    /** Silent for unknown, already verified or recently mailed addresses, so it cannot be used to probe accounts or mail-bomb one. */
    @Transactional
    public void resend(String email) {
        User user = this.usersRepository.findByEmail(EmailAddress.normalise(email)).orElse(null);
        if (user == null || user.isEmailVerified()) {
            return;
        }
        this.sendVerification(user);
    }

    @Transactional
    public void purgeStale(Duration unverifiedAccountMaxAge) {
        this.codeRepository.deleteExpired(Instant.now());
        this.usersRepository.deleteUnverifiedCreatedBefore(Instant.now().minus(unverifiedAccountMaxAge));
    }

    /** The account already exists by now; a mail failure must not turn sign-up into an error, the user can ask for a new code. */
    private void mailSafely(String email, String code) {
        try {
            this.mailer.sendCode(email, code);
        } catch (RuntimeException e) {
            log.error("Verification code for {} was not sent: {}", email, e.getMessage());
        }
    }

    private boolean canSend(EmailVerificationCode existing) {
        Instant now = Instant.now();
        if (existing.getLastSentAt().plus(RESEND_COOLDOWN).isAfter(now)) {
            return false;
        }
        boolean windowOpen = existing.getWindowStartedAt().plus(SEND_WINDOW).isAfter(now);
        return !windowOpen || existing.getSendsInWindow() < MAX_SENDS_PER_WINDOW;
    }

    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    /** Keyed (HMAC) and bound to the user, so equal codes of different users differ and nothing can be derived from the table alone. */
    private String hash(UUID userId, String code) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(this.hashSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal((userId + ":" + code).getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 is not available", e);
        }
    }
}
