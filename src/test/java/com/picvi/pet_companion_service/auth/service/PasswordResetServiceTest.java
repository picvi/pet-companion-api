package com.picvi.pet_companion_service.auth.service;

import com.picvi.pet_companion_service.auth.entity.PasswordResetCode;
import com.picvi.pet_companion_service.auth.repository.PasswordResetCodeRepository;
import com.picvi.pet_companion_service.exception.InvalidResetCode;
import com.picvi.pet_companion_service.exception.PasswordTooCommon;
import com.picvi.pet_companion_service.user.entity.User;
import com.picvi.pet_companion_service.user.repository.UsersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String EMAIL = "viki@example.com";

    @Mock
    private PasswordResetCodeRepository codeRepository;
    @Mock
    private UsersRepository usersRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private VerificationMailer mailer;
    @Mock
    private PasswordPolicy passwordPolicy;
    @InjectMocks
    private PasswordResetService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "hashSecret", "test-secret");
    }

    @Test
    void requestMailsSixDigitsWithTheUsernameAndStoresOnlyAHash() {
        User user = verifiedUser();
        when(usersRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        service.request("Viki@Example.com");

        PasswordResetCode stored = storedCode();
        ArgumentCaptor<String> mailed = ArgumentCaptor.forClass(String.class);
        verify(mailer).sendPasswordResetCode(eq(EMAIL), eq("viki"), mailed.capture());
        assertThat(mailed.getValue()).matches("\\d{6}");
        assertThat(stored.getCodeHash()).hasSize(64).isNotEqualTo(mailed.getValue());
    }

    @Test
    void requestIsSilentForUnknownEmail() {
        when(usersRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        service.request("ghost@example.com");

        verify(codeRepository, never()).save(any(PasswordResetCode.class));
        verify(mailer, never()).sendPasswordResetCode(anyString(), anyString(), anyString());
    }

    @Test
    void requestIsSilentForUnconfirmedAccount() {
        User user = verifiedUser();
        user.setEmailVerified(false);
        when(usersRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        service.request(EMAIL);

        verify(codeRepository, never()).save(any(PasswordResetCode.class));
        verify(mailer, never()).sendPasswordResetCode(anyString(), anyString(), anyString());
    }

    @Test
    void requestHonoursCooldown() {
        User user = verifiedUser();
        PasswordResetCode recent = new PasswordResetCode(user, "hash", Instant.now().plusSeconds(900));
        when(usersRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(codeRepository.findByUserId(USER_ID)).thenReturn(Optional.of(recent));

        service.request(EMAIL);

        verify(mailer, never()).sendPasswordResetCode(anyString(), anyString(), anyString());
        assertThat(recent.getCodeHash()).isEqualTo("hash");
    }

    @Test
    void requestAfterCooldownReplacesTheOldCodeAndResetsGuesses() {
        User user = verifiedUser();
        PasswordResetCode old = codeSentAt(user, Instant.now().minusSeconds(120), 1);
        old.recordWrongAttempt();
        when(usersRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(codeRepository.findByUserId(USER_ID)).thenReturn(Optional.of(old));

        service.request(EMAIL);

        verify(mailer).sendPasswordResetCode(eq(EMAIL), eq("viki"), anyString());
        assertThat(old.getAttempts()).isZero();
        assertThat(old.getSendsInWindow()).isEqualTo(2);
    }

    @Test
    void requestStopsAtTheHourlyCap() {
        User user = verifiedUser();
        PasswordResetCode capped = codeSentAt(user, Instant.now().minusSeconds(120), PasswordResetService.MAX_SENDS_PER_WINDOW);
        when(usersRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(codeRepository.findByUserId(USER_ID)).thenReturn(Optional.of(capped));

        service.request(EMAIL);

        verify(mailer, never()).sendPasswordResetCode(anyString(), anyString(), anyString());
    }

    @Test
    void resetWithTheMailedCodeSetsThePasswordSpendsTheCodeAndClosesEverySession() {
        User user = verifiedUser();
        String code = requestAndCaptureCode(user);
        PasswordResetCode stored = storedCode();
        when(codeRepository.findByUserId(USER_ID)).thenReturn(Optional.of(stored));
        when(passwordEncoder.encode("new-secret-2")).thenReturn("new-hash");

        User result = service.reset(EMAIL, code, "new-secret-2");

        assertThat(result).isSameAs(user);
        assertThat(user.getPassword_hash()).isEqualTo("new-hash");
        verify(codeRepository).deleteAllByUserId(USER_ID);
        verify(refreshTokenService).revokeAll(USER_ID);
        verify(mailer).sendPasswordChanged(EMAIL);
    }

    @Test
    void commonNewPasswordIsRejectedAndTheCodeStaysUnspent() {
        User user = verifiedUser();
        String code = requestAndCaptureCode(user);
        PasswordResetCode stored = storedCode();
        when(codeRepository.findByUserId(USER_ID)).thenReturn(Optional.of(stored));
        doThrow(new PasswordTooCommon()).when(passwordPolicy).check("password1", "viki", EMAIL);

        assertThatThrownBy(() -> service.reset(EMAIL, code, "password1")).isInstanceOf(PasswordTooCommon.class);

        verify(codeRepository, never()).deleteAllByUserId(USER_ID);
        verify(refreshTokenService, never()).revokeAll(USER_ID);
    }

    @Test
    void resetCountsWrongGuessesAndLocksAfterMaxAttempts() {
        User user = verifiedUser();
        String code = requestAndCaptureCode(user);
        PasswordResetCode stored = storedCode();
        String wrong = code.equals("000000") ? "111111" : "000000";
        when(codeRepository.findByUserId(USER_ID)).thenReturn(Optional.of(stored));

        for (int i = 0; i < PasswordResetService.MAX_ATTEMPTS; i++) {
            assertThatThrownBy(() -> service.reset(EMAIL, wrong, "new-secret-2")).isInstanceOf(InvalidResetCode.class);
        }

        assertThat(stored.getAttempts()).isEqualTo(PasswordResetService.MAX_ATTEMPTS);
        assertThatThrownBy(() -> service.reset(EMAIL, code, "new-secret-2")).isInstanceOf(InvalidResetCode.class);
        verify(passwordEncoder, never()).encode(anyString());
        verify(refreshTokenService, never()).revokeAll(any());
    }

    @Test
    void resetRejectsExpiredCode() {
        User user = verifiedUser();
        String code = requestAndCaptureCode(user);
        PasswordResetCode fresh = storedCode();
        PasswordResetCode expired = new PasswordResetCode(user, fresh.getCodeHash(), Instant.now().minusSeconds(1));
        when(codeRepository.findByUserId(USER_ID)).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.reset(EMAIL, code, "new-secret-2")).isInstanceOf(InvalidResetCode.class);
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void resetFailsTheSameWayForUnknownUnconfirmedAndCodelessAccounts() {
        User unconfirmed = verifiedUser();
        unconfirmed.setEmailVerified(false);
        when(usersRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());
        when(usersRepository.findByEmail(EMAIL)).thenReturn(Optional.of(unconfirmed));

        assertThatThrownBy(() -> service.reset("ghost@example.com", "123456", "new-secret-2")).isInstanceOf(InvalidResetCode.class);
        assertThatThrownBy(() -> service.reset(EMAIL, "123456", "new-secret-2")).isInstanceOf(InvalidResetCode.class);

        unconfirmed.setEmailVerified(true);
        when(codeRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.reset(EMAIL, "123456", "new-secret-2")).isInstanceOf(InvalidResetCode.class);
    }

    @Test
    void anEmailVerificationCodeHashDoesNotPassAsAResetCode() {
        User user = verifiedUser();
        String code = requestAndCaptureCode(user);
        PasswordResetCode stored = storedCode();
        EmailVerificationService verification = new EmailVerificationService(null, null, null);
        ReflectionTestUtils.setField(verification, "hashSecret", "test-secret");
        String verificationHash = ReflectionTestUtils.invokeMethod(verification, "hash", USER_ID, code);

        assertThat(stored.getCodeHash()).isNotEqualTo(verificationHash);
    }

    private String requestAndCaptureCode(User user) {
        when(usersRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        service.request(EMAIL);
        ArgumentCaptor<String> mailed = ArgumentCaptor.forClass(String.class);
        verify(mailer).sendPasswordResetCode(eq(EMAIL), eq("viki"), mailed.capture());
        return mailed.getValue();
    }

    private PasswordResetCode storedCode() {
        ArgumentCaptor<PasswordResetCode> stored = ArgumentCaptor.forClass(PasswordResetCode.class);
        verify(codeRepository).save(stored.capture());
        return stored.getValue();
    }

    private static PasswordResetCode codeSentAt(User user, Instant sentAt, int sends) {
        PasswordResetCode code = new PasswordResetCode(user, "hash", Instant.now().plusSeconds(900));
        ReflectionTestUtils.setField(code, "lastSentAt", sentAt);
        ReflectionTestUtils.setField(code, "windowStartedAt", sentAt);
        ReflectionTestUtils.setField(code, "sendsInWindow", sends);
        return code;
    }

    private static User verifiedUser() {
        User user = new User();
        user.setUsername("viki");
        user.setEmail(EMAIL);
        user.setEmailVerified(true);
        ReflectionTestUtils.setField(user, "id", USER_ID);
        return user;
    }
}
