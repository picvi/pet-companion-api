package com.picvi.pet_companion_service.auth.service;

import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificationMailerTest {
    @Mock
    private ObjectProvider<JavaMailSender> sender;
    @Mock
    private JavaMailSender mailSender;

    private VerificationMailer mailer;

    @BeforeEach
    void setUp() {
        mailer = new VerificationMailer(sender);
        ReflectionTestUtils.setField(mailer, "from", "curl up <no-reply@mail.curl.pet>");
    }

    @Test
    void htmlCarriesTheCodeAndNoLeftoverPlaceholder() {
        String html = mailer.html("482913");

        assertThat(html).contains("482913").doesNotContain("{{");
        assertThat(html.indexOf("482913")).isNotEqualTo(html.lastIndexOf("482913"));
    }

    @Test
    void plainTextCarriesTheCode() {
        assertThat(mailer.plainText("482913")).contains("482913").contains("curl up");
    }

    @Test
    void sendsOneMessageWithSubjectRecipientAndBothParts() throws Exception {
        when(sender.getIfAvailable()).thenReturn(mailSender);
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));

        mailer.sendCode("viki@example.com", "482913");

        ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(sent.capture());
        MimeMessage message = sent.getValue();
        message.saveChanges();
        assertThat(message.getSubject()).isEqualTo("Your curl up code: 482913");
        assertThat(message.getRecipients(Message.RecipientType.TO)[0].toString()).isEqualTo("viki@example.com");
        assertThat(message.getFrom()[0].toString()).contains("no-reply@mail.curl.pet");
        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        message.writeTo(raw);
        String mime = raw.toString();
        assertThat(mime).contains("text/plain").contains("text/html");
    }

    @Test
    void withoutMailConfigurationNothingIsSentButCodeIsLoggedInDebug() {
        when(sender.getIfAvailable()).thenReturn(null);
        ReflectionTestUtils.setField(mailer, "logCodes", true);

        mailer.sendCode("viki@example.com", "482913");

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void withoutMailConfigurationSendingFailsInsteadOfLoggingTheCode() {
        when(sender.getIfAvailable()).thenReturn(null);

        assertThatThrownBy(() -> mailer.sendCode("viki@example.com", "482913")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void passwordResetHtmlCarriesTheCodeAndTheEscapedUsername() {
        String html = mailer.passwordResetHtml("<viki>", "482913");

        assertThat(html).contains("482913").contains("&lt;viki&gt;").doesNotContain("{{").doesNotContain("<viki>");
    }

    @Test
    void passwordResetPlainTextCarriesTheCodeAndTheUsername() {
        assertThat(mailer.passwordResetPlainText("viki", "482913")).contains("482913").contains("viki");
    }

    @Test
    void passwordResetMailHasItsOwnSubject() throws Exception {
        when(sender.getIfAvailable()).thenReturn(mailSender);
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));

        mailer.sendPasswordResetCode("viki@example.com", "viki", "482913");

        ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(sent.capture());
        sent.getValue().saveChanges();
        assertThat(sent.getValue().getSubject()).isEqualTo("Your curl up password code: 482913");
    }

    @Test
    void passwordChangedMailNeedsNoCodeAndPointsToSupport() throws Exception {
        when(sender.getIfAvailable()).thenReturn(mailSender);
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));

        mailer.sendPasswordChanged("viki@example.com");

        verify(mailSender).send(any(MimeMessage.class));
        assertThat(mailer.passwordChangedPlainText()).contains("support@curl.pet");
    }

    @Test
    void passwordResetWithoutMailConfigurationFailsInsteadOfLoggingTheCode() {
        when(sender.getIfAvailable()).thenReturn(null);

        assertThatThrownBy(() -> mailer.sendPasswordResetCode("viki@example.com", "viki", "482913"))
                .isInstanceOf(IllegalStateException.class);
    }
}
