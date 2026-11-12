package com.picvi.pet_companion_service.auth.service;

import com.picvi.pet_companion_service.auth.entity.PasswordResetCode;
import com.picvi.pet_companion_service.auth.repository.PasswordResetCodeRepository;
import com.picvi.pet_companion_service.exception.InvalidResetCode;
import com.picvi.pet_companion_service.user.entity.User;
import com.picvi.pet_companion_service.user.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {
    static final int MAX_ATTEMPTS = 5;
    static final int MAX_SENDS_PER_WINDOW = 5;
    private static final Duration CODE_TTL = Duration.ofMinutes(15);
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    private static final Duration SEND_WINDOW = Duration.ofHours(1);
    private static final String HASH_PURPOSE = "password-reset";

    private final PasswordResetCodeRepository codeRepository;
    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final VerificationMailer mailer;
    private final PasswordPolicy passwordPolicy;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${spring.jwt.secret}")
    private String hashSecret;

    /**
     * Silent for unknown and unconfirmed addresses and inside the cooldown or hourly cap, so the answer never tells
     * whether an account exists and a mailbox cannot be flooded. A new code replaces the earlier one.
     */
    @Transactional
    public void request(String email) {
        User user = this.usersRepository.findByEmail(EmailAddress.normalise(email)).orElse(null);
        if (user == null || !user.isEmailVerified()) {
            return;
        }
        Instant now = Instant.now();
        PasswordResetCode existing = this.codeRepository.findByUserId(user.getId()).orElse(null);
        if (existing != null && !this.canSend(existing, now)) {
            return;
        }
        String code = String.format("%06d", this.secureRandom.nextInt(1_000_000));
        String hash = this.hash(user.getId(), code);
        Instant expiresAt = now.plus(CODE_TTL);
        if (existing == null) {
            this.codeRepository.save(new PasswordResetCode(user, hash, expiresAt));
        } else {
            existing.reissue(hash, expiresAt, now, SEND_WINDOW);
        }
        this.afterCommit(() -> this.mailSafely(() -> this.mailer.sendPasswordResetCode(user.getEmail(), user.getUsername(), code)));
    }

    /**
     * Every failure looks the same to the caller. A wrong guess is counted and kept despite the exception;
     * after MAX_ATTEMPTS the code is dead and a new one has to be requested. A correct code is spent at once
     * and every session of the account is closed.
     */
    @Transactional(noRollbackFor = InvalidResetCode.class)
    public User reset(String email, String code, String newPassword) {
        User user = this.usersRepository.findByEmail(EmailAddress.normalise(email)).orElse(null);
        if (user == null || !user.isEmailVerified()) {
            throw new InvalidResetCode();
        }
        PasswordResetCode stored = this.codeRepository.findByUserId(user.getId()).orElseThrow(InvalidResetCode::new);
        if (stored.getExpiresAt().isBefore(Instant.now()) || stored.getAttempts() >= MAX_ATTEMPTS) {
            throw new InvalidResetCode();
        }
        if (!MessageDigest.isEqual(
                stored.getCodeHash().getBytes(StandardCharsets.UTF_8),
                this.hash(user.getId(), code).getBytes(StandardCharsets.UTF_8))) {
            stored.recordWrongAttempt();
            throw new InvalidResetCode();
        }
        this.passwordPolicy.check(newPassword, user.getUsername(), user.getEmail());
        user.setPassword_hash(this.passwordEncoder.encode(newPassword));
        this.codeRepository.deleteAllByUserId(user.getId());
        this.refreshTokenService.revokeAll(user.getId());
        this.afterCommit(() -> this.mailSafely(() -> this.mailer.sendPasswordChanged(user.getEmail())));
        return user;
    }

    @Transactional
    public void purgeExpired() {
        this.codeRepository.deleteExpired(Instant.now());
    }

    private boolean canSend(PasswordResetCode existing, Instant now) {
        if (existing.getLastSentAt().plus(RESEND_COOLDOWN).isAfter(now)) {
            return false;
        }
        boolean windowOpen = existing.getWindowStartedAt().plus(SEND_WINDOW).isAfter(now);
        return !windowOpen || existing.getSendsInWindow() < MAX_SENDS_PER_WINDOW;
    }

    /** The reset already happened or the code is stored; a mail failure must not turn the call into an error. */
    private void mailSafely(Runnable send) {
        try {
            send.run();
        } catch (RuntimeException e) {
            log.error("A password mail was not sent: {}", e.getMessage());
        }
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

    /** Keyed (HMAC), bound to the user and to this purpose, so a verification code can never pass as a reset code. */
    private String hash(UUID userId, String code) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(this.hashSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal((HASH_PURPOSE + ":" + userId + ":" + code).getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 is not available", e);
        }
    }
}
