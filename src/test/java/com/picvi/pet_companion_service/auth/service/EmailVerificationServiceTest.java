package com.picvi.pet_companion_service.auth.service;

import com.picvi.pet_companion_service.auth.entity.EmailVerificationCode;
import com.picvi.pet_companion_service.auth.repository.EmailVerificationCodeRepository;
import com.picvi.pet_companion_service.exception.InvalidVerificationCode;
import com.picvi.pet_companion_service.user.entity.User;
import com.picvi.pet_companion_service.user.repository.UsersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private EmailVerificationCodeRepository codeRepository;
    @Mock
    private UsersRepository usersRepository;
    @Mock
    private VerificationMailer mailer;
    @InjectMocks
    private EmailVerificationService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "hashSecret", "test-secret");
    }

    @Test
    void sendVerificationMailsSixDigitsAndStoresOnlyAHash() {
        service.sendVerification(unverifiedUser());

        ArgumentCaptor<EmailVerificationCode> stored = ArgumentCaptor.forClass(EmailVerificationCode.class);
        ArgumentCaptor<String> mailed = ArgumentCaptor.forClass(String.class);
        verify(codeRepository).save(stored.capture());
        verify(mailer).sendCode(eq("viki@example.com"), mailed.capture());
        assertThat(mailed.getValue()).matches("\\d{6}");
        assertThat(stored.getValue().getCodeHash()).hasSize(64).isNotEqualTo(mailed.getValue());
    }

    @Test
    void verifyAcceptsTheMailedCode() {
        User user = unverifiedUser();
        String code = captureMailedCode(user);
        EmailVerificationCode stored = captureStored(user);
        when(usersRepository.findByEmail("viki@example.com")).thenReturn(Optional.of(user));
        when(codeRepository.findByUserId(USER_ID)).thenReturn(Optional.of(stored));

        User verified = service.verify("viki@example.com", code);

        assertThat(verified).isSameAs(user);
        assertThat(user.isEmailVerified()).isTrue();
    }

    @Test
    void verifyCountsWrongGuessesAndLocksAfterMaxAttempts() {
        User user = unverifiedUser();
        String code = captureMailedCode(user);
        EmailVerificationCode stored = captureStored(user);
        String wrong = code.equals("000000") ? "111111" : "000000";
        when(usersRepository.findByEmail("viki@example.com")).thenReturn(Optional.of(user));
        when(codeRepository.findByUserId(USER_ID)).thenReturn(Optional.of(stored));

        for (int i = 0; i < EmailVerificationService.MAX_ATTEMPTS; i++) {
            assertThatThrownBy(() -> service.verify("viki@example.com", wrong)).isInstanceOf(InvalidVerificationCode.class);
        }

        assertThat(stored.getAttempts()).isEqualTo(EmailVerificationService.MAX_ATTEMPTS);
        assertThatThrownBy(() -> service.verify("viki@example.com", code)).isInstanceOf(InvalidVerificationCode.class);
        assertThat(user.isEmailVerified()).isFalse();
    }

    @Test
    void verifyRejectsExpiredCode() {
        User user = unverifiedUser();
        String code = captureMailedCode(user);
        EmailVerificationCode fresh = captureStored(user);
        EmailVerificationCode expired = new EmailVerificationCode(user, fresh.getCodeHash(), Instant.now().minusSeconds(1));
        when(usersRepository.findByEmail("viki@example.com")).thenReturn(Optional.of(user));
        when(codeRepository.findByUserId(USER_ID)).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.verify("viki@example.com", code)).isInstanceOf(InvalidVerificationCode.class);
        assertThat(user.isEmailVerified()).isFalse();
    }

    @Test
    void verifyLooksTheSameForUnknownAndAlreadyVerifiedEmails() {
        User verified = unverifiedUser();
        verified.setEmailVerified(true);
        when(usersRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());
        when(usersRepository.findByEmail("viki@example.com")).thenReturn(Optional.of(verified));

        assertThatThrownBy(() -> service.verify("ghost@example.com", "123456")).isInstanceOf(InvalidVerificationCode.class);
        assertThatThrownBy(() -> service.verify("viki@example.com", "123456")).isInstanceOf(InvalidVerificationCode.class);
    }

    @Test
    void resendIsSilentForUnknownEmail() {
        when(usersRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        service.resend("ghost@example.com");

        verify(mailer, never()).sendCode(anyString(), anyString());
    }

    @Test
    void resendIsSilentForVerifiedUser() {
        User user = unverifiedUser();
        user.setEmailVerified(true);
        when(usersRepository.findByEmail("viki@example.com")).thenReturn(Optional.of(user));

        service.resend("viki@example.com");

        verify(codeRepository, never()).save(any(EmailVerificationCode.class));
    }

    @Test
    void resendHonoursCooldown() {
        User user = unverifiedUser();
        EmailVerificationCode recent = new EmailVerificationCode(user, "hash", Instant.now().plusSeconds(900));
        when(usersRepository.findByEmail("viki@example.com")).thenReturn(Optional.of(user));
        when(codeRepository.findByUserId(USER_ID)).thenReturn(Optional.of(recent));

        service.resend("Viki@Example.com");

        verify(mailer, never()).sendCode(anyString(), anyString());
        assertThat(recent.getCodeHash()).isEqualTo("hash");
    }

    @Test
    void resendIssuesNewCodeAfterCooldownAndResetsGuesses() {
        User user = unverifiedUser();
        EmailVerificationCode old = codeSentAt(user, Instant.now().minusSeconds(120), 1);
        old.recordWrongAttempt();
        when(usersRepository.findByEmail("viki@example.com")).thenReturn(Optional.of(user));
        when(codeRepository.findByUserId(USER_ID)).thenReturn(Optional.of(old));

        service.resend("viki@example.com");

        verify(mailer).sendCode(eq("viki@example.com"), anyString());
        assertThat(old.getAttempts()).isZero();
        assertThat(old.getSendsInWindow()).isEqualTo(2);
    }

    @Test
    void resendStopsAtTheHourlyCapSoGuessesCannotBeRefreshedForever() {
        User user = unverifiedUser();
        EmailVerificationCode capped = codeSentAt(user, Instant.now().minusSeconds(120), EmailVerificationService.MAX_SENDS_PER_WINDOW);
        when(usersRepository.findByEmail("viki@example.com")).thenReturn(Optional.of(user));
        when(codeRepository.findByUserId(USER_ID)).thenReturn(Optional.of(capped));

        service.resend("viki@example.com");

        verify(mailer, never()).sendCode(anyString(), anyString());
    }

    private static EmailVerificationCode codeSentAt(User user, Instant sentAt, int sends) {
        EmailVerificationCode code = new EmailVerificationCode(user, "hash", Instant.now().plusSeconds(900));
        ReflectionTestUtils.setField(code, "lastSentAt", sentAt);
        ReflectionTestUtils.setField(code, "windowStartedAt", sentAt);
        ReflectionTestUtils.setField(code, "sendsInWindow", sends);
        return code;
    }

    private String captureMailedCode(User user) {
        service.sendVerification(user);
        ArgumentCaptor<String> mailed = ArgumentCaptor.forClass(String.class);
        verify(mailer).sendCode(eq(user.getEmail()), mailed.capture());
        return mailed.getValue();
    }

    private EmailVerificationCode captureStored(User user) {
        ArgumentCaptor<EmailVerificationCode> stored = ArgumentCaptor.forClass(EmailVerificationCode.class);
        verify(codeRepository).save(stored.capture());
        return stored.getValue();
    }

    private static User unverifiedUser() {
        User user = new User();
        user.setUsername("viki");
        user.setEmail("viki@example.com");
        user.setEmailVerified(false);
        ReflectionTestUtils.setField(user, "id", USER_ID);
        return user;
    }
}
