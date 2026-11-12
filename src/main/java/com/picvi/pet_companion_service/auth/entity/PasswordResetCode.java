package com.picvi.pet_companion_service.auth.entity;

import com.picvi.pet_companion_service.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "password_reset_codes")
public class PasswordResetCode {
    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Getter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Getter
    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Getter
    @Column(nullable = false)
    private int attempts;

    @Getter
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Getter
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Getter
    @Column(name = "last_sent_at", nullable = false)
    private Instant lastSentAt;

    @Getter
    @Column(name = "window_started_at", nullable = false)
    private Instant windowStartedAt;

    @Getter
    @Column(name = "sends_in_window", nullable = false)
    private int sendsInWindow;

    protected PasswordResetCode() {
    }

    public PasswordResetCode(User user, String codeHash, Instant expiresAt) {
        this.user = user;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
        this.lastSentAt = this.createdAt;
        this.windowStartedAt = this.createdAt;
        this.sendsInWindow = 1;
    }

    /** A new code replaces the old one in place: guesses start again, but the send counters carry on. */
    public void reissue(String codeHash, Instant expiresAt, Instant now, Duration window) {
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.attempts = 0;
        if (this.windowStartedAt.plus(window).isBefore(now)) {
            this.windowStartedAt = now;
            this.sendsInWindow = 1;
        } else {
            this.sendsInWindow++;
        }
        this.lastSentAt = now;
    }

    public void recordWrongAttempt() {
        this.attempts++;
    }
}
