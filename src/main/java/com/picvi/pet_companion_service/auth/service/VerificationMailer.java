package com.picvi.pet_companion_service.auth.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Sends over SMTP when spring.mail.host is set; without it a code is only logged if pawstep.auth.log-codes is on (local dev), otherwise sending fails. */
@Slf4j
@Component
@RequiredArgsConstructor
public class VerificationMailer {
    private static final String VERIFICATION_TEMPLATE = "mail/verification-code.html";
    private static final String RESET_TEMPLATE = "mail/password-reset-code.html";
    private static final String PASSWORD_CHANGED_TEMPLATE = "mail/password-changed.html";
    private static final String CODE_PLACEHOLDER = "{{code}}";
    private static final String USERNAME_PLACEHOLDER = "{{username}}";

    private final ObjectProvider<JavaMailSender> sender;

    @Value("${pawstep.auth.mail-from:}")
    private String from;

    /** Only for local debugging: without a mail server the code is written to the log instead. */
    @Value("${pawstep.auth.log-codes:false}")
    private boolean logCodes;

    private final Map<String, String> templates = new ConcurrentHashMap<>();

    public void sendCode(String to, String code) {
        this.sendCodeMail(to, code, "Your curl up code: " + code, this.plainText(code), this.html(code));
    }

    public void sendPasswordResetCode(String to, String username, String code) {
        this.sendCodeMail(to, code, "Your curl up password code: " + code,
                this.passwordResetPlainText(username, code), this.passwordResetHtml(username, code));
    }

    public void sendPasswordChanged(String to) {
        this.send(to, "Your curl up password was changed", this.passwordChangedPlainText(), this.template(PASSWORD_CHANGED_TEMPLATE));
    }

    String html(String code) {
        return this.template(VERIFICATION_TEMPLATE).replace(CODE_PLACEHOLDER, code);
    }

    String plainText(String code) {
        return "Your curl up code: " + code + "\n\n"
                + "Type it in the app to finish creating your account. It works for 15 minutes, one time only.\n\n"
                + "Code expired? Open the app and tap \"Send a new code\".\n\n"
                + "Did not sign up for curl up? Ignore this email. Nothing is created until the code is entered.\n";
    }

    String passwordResetHtml(String username, String code) {
        return this.template(RESET_TEMPLATE)
                .replace(CODE_PLACEHOLDER, code)
                .replace(USERNAME_PLACEHOLDER, HtmlUtils.htmlEscape(username));
    }

    String passwordResetPlainText(String username, String code) {
        return "Your curl up password code: " + code + "\n\n"
                + "Type it in the app to choose a new password. It works for 15 minutes, one time only.\n\n"
                + "Your username: " + username + "\n\n"
                + "Did not ask for this? Ignore this email. Your password stays the same.\n";
    }

    String passwordChangedPlainText() {
        return "The password of your curl up account was changed, and every device was signed out.\n\n"
                + "Was it you? Nothing else to do.\n\n"
                + "Was it not you? Write to support@curl.pet right away.\n";
    }

    private void sendCodeMail(String to, String code, String subject, String text, String html) {
        if (this.sender.getIfAvailable() == null && this.logCodes) {
            log.warn("Mail is not configured; code for {}: {}", to, code);
            return;
        }
        this.send(to, subject, text, html);
    }

    private void send(String to, String subject, String text, String html) {
        JavaMailSender mailSender = this.sender.getIfAvailable();
        if (mailSender == null) {
            throw new IllegalStateException("Mail is not configured (spring.mail.host), cannot send the message");
        }
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            if (!this.from.isBlank()) {
                helper.setFrom(this.from);
            }
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, html);
        } catch (MessagingException e) {
            throw new MailPreparationException(e);
        }
        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.error("Could not send a mail to {}", to, e);
            throw e;
        }
    }

    private String template(String name) {
        return this.templates.computeIfAbsent(name, key -> {
            try (var in = new ClassPathResource(key).getInputStream()) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException("Cannot read " + key, e);
            }
        });
    }
}
