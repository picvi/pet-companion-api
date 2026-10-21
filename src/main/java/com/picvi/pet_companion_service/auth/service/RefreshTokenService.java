package com.picvi.pet_companion_service.auth.service;

import com.picvi.pet_companion_service.auth.entity.RefreshToken;
import com.picvi.pet_companion_service.auth.repository.RefreshTokenRepository;
import com.picvi.pet_companion_service.exception.InvalidRefreshToken;
import com.picvi.pet_companion_service.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${spring.jwt.refresh-token-expiration}")
    private Duration refreshTokenExpiration;

    public String issue(User user) {
        String rawToken = this.generateRawToken();
        this.refreshTokenRepository.save(
                new RefreshToken(user, this.hash(rawToken), Instant.now().plus(this.refreshTokenExpiration))
        );
        return rawToken;
    }

    public User rotate(String rawToken) {
        RefreshToken storedToken = this.findStoredToken(rawToken);

        if (storedToken.isRevoked()) {
            this.revokeAll(storedToken.getUser().getId());
            throw new InvalidRefreshToken();
        }
        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidRefreshToken();
        }

        storedToken.setRevoked(true);
        return storedToken.getUser();
    }

    public void revoke(String rawToken) {
        this.refreshTokenRepository.findByTokenHash(this.hash(rawToken))
                .ifPresent(token -> token.setRevoked(true));
    }

    public void deleteAll(UUID userId) {
        this.refreshTokenRepository.deleteAllByUserId(userId);
    }

    private RefreshToken findStoredToken(String rawToken) {
        return this.refreshTokenRepository.findByTokenHash(this.hash(rawToken))
                .orElseThrow(InvalidRefreshToken::new);
    }

    public void revokeAll(UUID userId) {
        this.refreshTokenRepository.findAllByUserIdAndRevokedFalse(userId)
                .forEach(token -> token.setRevoked(true));
    }

    private String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        this.secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
