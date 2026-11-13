package com.picvi.pet_companion_service.account.service;

import com.picvi.pet_companion_service.account.dto.ChangePasswordRequest;
import com.picvi.pet_companion_service.auth.service.AuthService;
import com.picvi.pet_companion_service.auth.service.LoginAttemptLimiter;
import com.picvi.pet_companion_service.auth.service.PasswordPolicy;
import com.picvi.pet_companion_service.auth.service.RefreshTokenService;
import com.picvi.pet_companion_service.exception.TooManyLoginAttempts;
import com.picvi.pet_companion_service.exception.account.CurrentPasswordIncorrect;
import com.picvi.pet_companion_service.nutrition.api.NutritionApi;
import com.picvi.pet_companion_service.pet.service.PetService;
import com.picvi.pet_companion_service.user.entity.User;
import com.picvi.pet_companion_service.user.repository.UsersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;

/** Password change and account deletion share one per-user lock (5 failures, 15 minutes). */
@ExtendWith(MockitoExtension.class)
class ReauthAttemptLimitTest {
    private static final UUID VIKI = UUID.randomUUID();
    private static final UUID MARTA = UUID.randomUUID();
    private static final String HASH = "hash";
    private static final String RIGHT = "old-secret-1";

    @Mock
    private UsersRepository usersRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private AuthService authService;
    @Mock
    private PasswordPolicy passwordPolicy;
    @Mock
    private NutritionApi nutritionApi;
    @Mock
    private PetService petService;

    private PasswordService passwordService;
    private AccountDeletionService deletionService;

    @BeforeEach
    void setUp() {
        LoginAttemptLimiter shared = new LoginAttemptLimiter(Clock.systemUTC(), 5, Duration.ofMinutes(15));
        this.passwordService = new PasswordService(this.usersRepository, this.passwordEncoder,
                this.refreshTokenService, this.authService, this.passwordPolicy, shared);
        this.deletionService = new AccountDeletionService(this.usersRepository, this.nutritionApi, this.petService,
                this.refreshTokenService, this.passwordEncoder, shared);
        for (UUID id : new UUID[]{VIKI, MARTA}) {
            User user = new User();
            user.setPassword_hash(HASH);
            lenient().when(this.usersRepository.findById(id)).thenReturn(Optional.of(user));
        }
        lenient().when(this.passwordEncoder.matches(anyString(), eq(HASH)))
                .thenAnswer(call -> RIGHT.equals(call.getArgument(0)));
    }

    @Test
    void fiveWrongDeletionsLockPasswordChange() {
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> this.deletionService.delete(VIKI, "wrong-pass"))
                    .isInstanceOf(CurrentPasswordIncorrect.class);
        }

        assertThatThrownBy(() -> this.passwordService.change(VIKI, new ChangePasswordRequest(RIGHT, "new-secret-2")))
                .isInstanceOf(TooManyLoginAttempts.class);
        assertThatThrownBy(() -> this.deletionService.delete(VIKI, RIGHT))
                .isInstanceOf(TooManyLoginAttempts.class);
        verifyNoInteractions(this.nutritionApi, this.petService);
    }

    @Test
    void fiveWrongPasswordChangesLockDeletion() {
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> this.passwordService.change(VIKI, new ChangePasswordRequest("wrong-pass", "new-secret-2")))
                    .isInstanceOf(CurrentPasswordIncorrect.class);
        }

        assertThatThrownBy(() -> this.deletionService.delete(VIKI, RIGHT))
                .isInstanceOf(TooManyLoginAttempts.class);
        verifyNoInteractions(this.nutritionApi, this.petService);
    }

    @Test
    void anotherUserIsNotAffected() {
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> this.deletionService.delete(VIKI, "wrong-pass"))
                    .isInstanceOf(CurrentPasswordIncorrect.class);
        }

        assertThatCode(() -> this.deletionService.delete(MARTA, RIGHT)).doesNotThrowAnyException();
    }
}
