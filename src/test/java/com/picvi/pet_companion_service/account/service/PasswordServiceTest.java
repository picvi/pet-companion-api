package com.picvi.pet_companion_service.account.service;

import com.picvi.pet_companion_service.account.dto.ChangePasswordRequest;
import com.picvi.pet_companion_service.auth.dto.LoginResponse;
import com.picvi.pet_companion_service.auth.service.AuthService;
import com.picvi.pet_companion_service.auth.service.PasswordPolicy;
import com.picvi.pet_companion_service.exception.PasswordTooCommon;
import com.picvi.pet_companion_service.auth.service.RefreshTokenService;
import com.picvi.pet_companion_service.exception.UserDoesNotExist;
import com.picvi.pet_companion_service.exception.account.CurrentPasswordIncorrect;
import com.picvi.pet_companion_service.exception.account.PasswordUnchanged;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordServiceTest {
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String OLD_HASH = "old-hash";

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

    private PasswordService service;
    private User user;

    @BeforeEach
    void setUp() {
        this.service = new PasswordService(this.usersRepository, this.passwordEncoder, this.refreshTokenService, this.authService, this.passwordPolicy);
        this.user = new User();
        this.user.setPassword_hash(OLD_HASH);
        lenient().when(this.usersRepository.findById(USER_ID)).thenReturn(Optional.of(this.user));
    }

    @Test
    void wrongCurrentPasswordChangesNothing() {
        when(this.passwordEncoder.matches("wrong-pass", OLD_HASH)).thenReturn(false);

        assertThatThrownBy(() -> this.service.change(USER_ID, new ChangePasswordRequest("wrong-pass", "new-secret-2")))
                .isInstanceOf(CurrentPasswordIncorrect.class);

        assertThat(this.user.getPassword_hash()).isEqualTo(OLD_HASH);
        verifyNoInteractions(this.refreshTokenService, this.authService);
    }

    @Test
    void samePasswordIsRejected() {
        when(this.passwordEncoder.matches("old-secret-1", OLD_HASH)).thenReturn(true);

        assertThatThrownBy(() -> this.service.change(USER_ID, new ChangePasswordRequest("old-secret-1", "old-secret-1")))
                .isInstanceOf(PasswordUnchanged.class);

        verify(this.refreshTokenService, never()).revokeAll(USER_ID);
    }

    @Test
    void commonNewPasswordIsRejectedAndNothingChanges() {
        when(this.passwordEncoder.matches("old-secret-1", OLD_HASH)).thenReturn(true);
        doThrow(new PasswordTooCommon()).when(this.passwordPolicy).check("password1", null, null);

        assertThatThrownBy(() -> this.service.change(USER_ID, new ChangePasswordRequest("old-secret-1", "password1")))
                .isInstanceOf(PasswordTooCommon.class);

        assertThat(this.user.getPassword_hash()).isEqualTo(OLD_HASH);
        verify(this.refreshTokenService, never()).revokeAll(USER_ID);
    }

    @Test
    void changesHashRevokesOldSessionsThenIssuesNewOne() {
        LoginResponse session = mock(LoginResponse.class);
        when(this.passwordEncoder.matches("old-secret-1", OLD_HASH)).thenReturn(true);
        when(this.passwordEncoder.encode("new-secret-2")).thenReturn("new-hash");
        when(this.authService.issueSession(this.user)).thenReturn(session);

        LoginResponse result = this.service.change(USER_ID, new ChangePasswordRequest("old-secret-1", "new-secret-2"));

        assertThat(result).isSameAs(session);
        assertThat(this.user.getPassword_hash()).isEqualTo("new-hash");
        InOrder order = inOrder(this.refreshTokenService, this.authService);
        order.verify(this.refreshTokenService).revokeAll(USER_ID);
        order.verify(this.authService).issueSession(this.user);
    }

    @Test
    void unknownUserIsReported() {
        UUID other = UUID.randomUUID();
        when(this.usersRepository.findById(other)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> this.service.change(other, new ChangePasswordRequest("old-secret-1", "new-secret-2")))
                .isInstanceOf(UserDoesNotExist.class);
    }
}
