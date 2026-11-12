package com.picvi.pet_companion_service.auth.service;

import com.picvi.pet_companion_service.auth.dto.LoginRequest;
import com.picvi.pet_companion_service.auth.dto.LoginResponse;
import com.picvi.pet_companion_service.auth.dto.RegisterRequest;
import com.picvi.pet_companion_service.auth.dto.RegisterResponse;
import com.picvi.pet_companion_service.exception.EmailNotVerified;
import com.picvi.pet_companion_service.exception.InvalidCredentials;
import com.picvi.pet_companion_service.exception.PasswordTooCommon;
import com.picvi.pet_companion_service.exception.TooManyLoginAttempts;
import com.picvi.pet_companion_service.exception.UserAlreadyExistsException;
import com.picvi.pet_companion_service.pet.repository.PetRepository;
import com.picvi.pet_companion_service.user.entity.User;
import com.picvi.pet_companion_service.user.repository.UsersRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UsersRepository usersRepository;
    @Mock
    private PetRepository petRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private EmailVerificationService emailVerificationService;
    @Mock
    private PasswordResetService passwordResetService;
    @Mock
    private LoginAttemptLimiter loginAttemptLimiter;
    @Mock
    private PasswordPolicy passwordPolicy;
    @InjectMocks
    private AuthService authService;

    @Test
    void registerStoresUnverifiedUserAndSendsVerification() {
        when(usersRepository.findByEmail("viki@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret123")).thenReturn("hash");
        when(usersRepository.save(any(User.class))).thenAnswer(invocation -> withId(invocation.getArgument(0)));

        RegisterResponse response = authService.register(new RegisterRequest("viki", "viki@example.com", "secret123"));

        assertThat(response.email()).isEqualTo("viki@example.com");
        assertThat(response.verificationRequired()).isTrue();
        verify(emailVerificationService).sendVerification(any(User.class));
        verify(refreshTokenService, never()).issue(any(User.class));
    }

    @Test
    void registerLowercasesEmail() {
        when(usersRepository.findByEmail("viki@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret123")).thenReturn("hash");
        when(usersRepository.save(any(User.class))).thenAnswer(invocation -> withId(invocation.getArgument(0)));

        RegisterResponse response = authService.register(new RegisterRequest("viki", "Viki@Example.COM", "secret123"));

        assertThat(response.email()).isEqualTo("viki@example.com");
    }

    @Test
    void registerInDebugModeVerifiesAtOnceAndSendsNoCode() {
        ReflectionTestUtils.setField(authService, "emailVerificationRequired", false);
        when(passwordEncoder.encode("secret123")).thenReturn("hash");
        when(usersRepository.save(any(User.class))).thenAnswer(invocation -> withId(invocation.getArgument(0)));

        RegisterResponse response = authService.register(new RegisterRequest("viki", "viki@example.com", "secret123"));

        assertThat(response.verificationRequired()).isFalse();
        verify(emailVerificationService, never()).sendVerification(any(User.class));
        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(usersRepository).save(saved.capture());
        assertThat(saved.getValue().isEmailVerified()).isTrue();
    }

    @Test
    void verifyEmailLogsTheUserIn() {
        User user = existingUser();
        when(emailVerificationService.verify("viki@example.com", "123456")).thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn("access");
        when(refreshTokenService.issue(user)).thenReturn("refresh");
        when(petRepository.existsByOwnerId(USER_ID)).thenReturn(false);

        LoginResponse response = authService.verifyEmail("viki@example.com", "123456");

        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isEqualTo("refresh");
        assertThat(response.user().email()).isEqualTo("viki@example.com");
    }

    @Test
    void resetPasswordLogsTheUserIn() {
        User user = existingUser();
        when(passwordResetService.reset("viki@example.com", "123456", "new-secret-2")).thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn("access");
        when(refreshTokenService.issue(user)).thenReturn("refresh");
        when(petRepository.existsByOwnerId(USER_ID)).thenReturn(false);

        LoginResponse response = authService.resetPassword("viki@example.com", "123456", "new-secret-2");

        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isEqualTo("refresh");
    }

    @Test
    void resetPasswordClearsTheLoginLock() {
        User user = existingUser();
        when(passwordResetService.reset("viki@example.com", "123456", "new-secret-2")).thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn("access");
        when(refreshTokenService.issue(user)).thenReturn("refresh");
        when(petRepository.existsByOwnerId(USER_ID)).thenReturn(false);

        authService.resetPassword("viki@example.com", "123456", "new-secret-2");

        verify(loginAttemptLimiter).recordSuccess(user.getUsername());
    }

    @Test
    void loginRejectsUnverifiedEmail() {
        User user = existingUser();
        user.setEmailVerified(false);
        when(usersRepository.findByUsername("viki")).thenReturn(user);
        when(passwordEncoder.matches("secret123", "hash")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest("viki", "secret123")))
                .isInstanceOfSatisfying(EmailNotVerified.class,
                        e -> assertThat(e.getEmail()).isEqualTo("viki@example.com"));
        verify(refreshTokenService, never()).issue(any(User.class));
    }

    @Test
    void registerRejectsTakenUsername() {
        when(usersRepository.findByEmail("viki@example.com")).thenReturn(Optional.empty());
        when(usersRepository.findByUsername("viki")).thenReturn(existingUser());

        assertThatThrownBy(() -> authService.register(new RegisterRequest("viki", "viki@example.com", "secret123")))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessage("Username already exists");
        verify(usersRepository, never()).save(any(User.class));
    }

    @Test
    void registerWithVerifiedEmailLooksLikeASignUpButCreatesNothing() {
        when(usersRepository.findByEmail("viki@example.com")).thenReturn(Optional.of(existingUser()));

        RegisterResponse response = authService.register(new RegisterRequest("other", "viki@example.com", "secret123"));

        assertThat(response.verificationRequired()).isTrue();
        verify(usersRepository, never()).save(any(User.class));
        verify(emailVerificationService, never()).sendVerification(any(User.class));
    }

    @Test
    void registerTakesOverUnverifiedAccountOnlyWhenANewCodeGoesOut() {
        User pending = existingUser();
        pending.setEmailVerified(false);
        when(usersRepository.findByEmail("viki@example.com")).thenReturn(Optional.of(pending));
        when(passwordEncoder.encode("newsecret1")).thenReturn("newhash");
        when(emailVerificationService.canSend(pending)).thenReturn(true);

        authService.register(new RegisterRequest("viki2", "viki@example.com", "newsecret1"));

        assertThat(pending.getUsername()).isEqualTo("viki2");
        assertThat(pending.getPassword_hash()).isEqualTo("newhash");
        verify(emailVerificationService).sendVerification(pending);
    }

    @Test
    void registerKeepsCredentialsOfUnverifiedAccountWhileTheCodeIsThrottled() {
        User pending = existingUser();
        pending.setEmailVerified(false);
        when(usersRepository.findByEmail("viki@example.com")).thenReturn(Optional.of(pending));
        when(emailVerificationService.canSend(pending)).thenReturn(false);

        authService.register(new RegisterRequest("viki2", "viki@example.com", "newsecret1"));

        assertThat(pending.getUsername()).isEqualTo("viki");
        assertThat(pending.getPassword_hash()).isEqualTo("hash");
        verify(emailVerificationService, never()).sendVerification(any(User.class));
    }

    @Test
    void loginRejectsWrongPassword() {
        User user = existingUser();
        when(usersRepository.findByUsername("viki")).thenReturn(user);
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("viki", "wrong")))
                .isInstanceOf(InvalidCredentials.class);
        verify(loginAttemptLimiter).recordFailure("viki");
        verify(refreshTokenService, never()).issue(any(User.class));
    }

    @Test
    void loginRejectsUnknownUser() {
        when(usersRepository.findByUsername("ghost")).thenReturn(null);
        when(passwordEncoder.encode(anyString())).thenReturn("dummy-hash");

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost", "secret123")))
                .isInstanceOf(InvalidCredentials.class);
        verify(passwordEncoder).matches("secret123", "dummy-hash");
        verify(loginAttemptLimiter).recordFailure("ghost");
    }

    @Test
    void loginIsRefusedWhileTheAccountIsLocked() {
        doThrow(new TooManyLoginAttempts(60)).when(loginAttemptLimiter).checkAllowed("viki");

        assertThatThrownBy(() -> authService.login(new LoginRequest("viki", "secret123")))
                .isInstanceOf(TooManyLoginAttempts.class);
        verifyNoInteractions(usersRepository, passwordEncoder);
    }

    @Test
    void registerRejectsACommonPassword() {
        doThrow(new PasswordTooCommon()).when(passwordPolicy).check("password1", "viki", "viki@example.com");

        assertThatThrownBy(() -> authService.register(new RegisterRequest("viki", "viki@example.com", "password1")))
                .isInstanceOf(PasswordTooCommon.class);
        verify(usersRepository, never()).save(any(User.class));
    }

    @Test
    void loginReturnsTokensForValidCredentials() {
        User user = existingUser();
        when(usersRepository.findByUsername("viki")).thenReturn(user);
        when(passwordEncoder.matches("secret123", "hash")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("access");
        when(refreshTokenService.issue(user)).thenReturn("refresh");
        when(petRepository.existsByOwnerId(USER_ID)).thenReturn(true);

        LoginResponse response = authService.login(new LoginRequest("viki", "secret123"));
        verify(loginAttemptLimiter).recordSuccess("viki");

        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isEqualTo("refresh");
        assertThat(response.user().hasPet()).isTrue();
    }

    private User existingUser() {
        User user = new User();
        user.setUsername("viki");
        user.setEmail("viki@example.com");
        user.setPassword_hash("hash");
        user.setEmailVerified(true);
        return withId(user);
    }

    private static User withId(User user) {
        ReflectionTestUtils.setField(user, "id", USER_ID);
        return user;
    }
}
