package com.picvi.pet_companion_service.account.service;

import com.picvi.pet_companion_service.auth.service.LoginAttemptLimiter;
import com.picvi.pet_companion_service.auth.service.RefreshTokenService;
import com.picvi.pet_companion_service.exception.TooManyLoginAttempts;
import com.picvi.pet_companion_service.exception.UserDoesNotExist;
import com.picvi.pet_companion_service.exception.account.CurrentPasswordIncorrect;
import com.picvi.pet_companion_service.nutrition.api.NutritionApi;
import com.picvi.pet_companion_service.pet.service.PetService;
import com.picvi.pet_companion_service.user.entity.User;
import com.picvi.pet_companion_service.user.repository.UsersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountDeletionServiceTest {
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String KEY = USER_ID.toString();
    private static final String HASH = "hash";

    @Mock
    private UsersRepository usersRepository;
    @Mock
    private NutritionApi nutritionApi;
    @Mock
    private PetService petService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private LoginAttemptLimiter reauthAttemptLimiter;

    private AccountDeletionService service;

    @BeforeEach
    void setUp() {
        this.service = new AccountDeletionService(this.usersRepository, this.nutritionApi, this.petService,
                this.refreshTokenService, this.passwordEncoder, this.reauthAttemptLimiter);
        User user = new User();
        user.setPassword_hash(HASH);
        lenient().when(this.usersRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    }

    @Test
    void correctPasswordRemovesEverythingInOrderAndClearsTheCounter() {
        when(this.passwordEncoder.matches("old-secret-1", HASH)).thenReturn(true);
        when(this.petService.deleteAllOwnedBy(USER_ID)).thenReturn(1);

        this.service.delete(USER_ID, "old-secret-1");

        InOrder order = inOrder(this.reauthAttemptLimiter, this.nutritionApi, this.petService,
                this.refreshTokenService, this.usersRepository);
        order.verify(this.reauthAttemptLimiter).checkAllowed(KEY);
        order.verify(this.reauthAttemptLimiter).recordSuccess(KEY);
        order.verify(this.nutritionApi).forgetUser(USER_ID);
        order.verify(this.petService).deleteAllOwnedBy(USER_ID);
        order.verify(this.refreshTokenService).deleteAll(USER_ID);
        order.verify(this.usersRepository).deleteById(USER_ID);
    }

    @Test
    void wrongPasswordRecordsAFailureAndDeletesNothing() {
        when(this.passwordEncoder.matches("wrong-pass", HASH)).thenReturn(false);

        assertThatThrownBy(() -> this.service.delete(USER_ID, "wrong-pass"))
                .isInstanceOf(CurrentPasswordIncorrect.class);

        verify(this.reauthAttemptLimiter).recordFailure(KEY);
        verify(this.reauthAttemptLimiter, never()).recordSuccess(KEY);
        verify(this.usersRepository, never()).deleteById(USER_ID);
        verifyNoInteractions(this.nutritionApi, this.petService, this.refreshTokenService);
    }

    @Test
    void lockedUserIsRefusedBeforeThePasswordIsCompared() {
        doThrow(new TooManyLoginAttempts(60)).when(this.reauthAttemptLimiter).checkAllowed(KEY);

        assertThatThrownBy(() -> this.service.delete(USER_ID, "old-secret-1"))
                .isInstanceOf(TooManyLoginAttempts.class);

        verifyNoInteractions(this.passwordEncoder, this.nutritionApi, this.petService, this.refreshTokenService);
        verify(this.usersRepository, never()).deleteById(USER_ID);
    }

    @Test
    void unknownUserTouchesNothing() {
        UUID other = UUID.randomUUID();
        when(this.usersRepository.findById(other)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> this.service.delete(other, "old-secret-1")).isInstanceOf(UserDoesNotExist.class);

        verifyNoInteractions(this.passwordEncoder, this.nutritionApi, this.petService, this.refreshTokenService);
    }
}
