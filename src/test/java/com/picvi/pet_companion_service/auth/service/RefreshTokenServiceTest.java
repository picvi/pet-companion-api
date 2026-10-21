package com.picvi.pet_companion_service.auth.service;

import com.picvi.pet_companion_service.auth.entity.RefreshToken;
import com.picvi.pet_companion_service.auth.repository.RefreshTokenRepository;
import com.picvi.pet_companion_service.exception.InvalidRefreshToken;
import com.picvi.pet_companion_service.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenService refreshTokenService;
    private User user;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(refreshTokenRepository);
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenExpiration", Duration.ofDays(30));
        user = new User();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
    }

    @Test
    void rotateRevokesPresentedTokenAndReturnsUser() {
        String rawToken = refreshTokenService.issue(user);
        RefreshToken stored = capturedToken();
        when(refreshTokenRepository.findByTokenHash(stored.getTokenHash())).thenReturn(Optional.of(stored));

        User rotatedUser = refreshTokenService.rotate(rawToken);

        assertThat(rotatedUser).isSameAs(user);
        assertThat(stored.isRevoked()).isTrue();
    }

    @Test
    void rotateRevokesAllSessionsWhenRevokedTokenIsReused() {
        String rawToken = refreshTokenService.issue(user);
        RefreshToken stored = capturedToken();
        stored.setRevoked(true);
        RefreshToken otherSession = new RefreshToken(user, "other-hash", Instant.now().plusSeconds(60));
        when(refreshTokenRepository.findByTokenHash(stored.getTokenHash())).thenReturn(Optional.of(stored));
        when(refreshTokenRepository.findAllByUserIdAndRevokedFalse(user.getId())).thenReturn(List.of(otherSession));

        assertThatThrownBy(() -> refreshTokenService.rotate(rawToken)).isInstanceOf(InvalidRefreshToken.class);
        assertThat(otherSession.isRevoked()).isTrue();
    }

    @Test
    void rotateRejectsExpiredToken() {
        String rawToken = refreshTokenService.issue(user);
        RefreshToken stored = capturedToken();
        ReflectionTestUtils.setField(stored, "expiresAt", Instant.now().minusSeconds(1));
        when(refreshTokenRepository.findByTokenHash(stored.getTokenHash())).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> refreshTokenService.rotate(rawToken)).isInstanceOf(InvalidRefreshToken.class);
        assertThat(stored.isRevoked()).isFalse();
    }

    @Test
    void rotateRejectsUnknownToken() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.rotate("unknown")).isInstanceOf(InvalidRefreshToken.class);
    }

    private RefreshToken capturedToken() {
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        return captor.getValue();
    }
}
