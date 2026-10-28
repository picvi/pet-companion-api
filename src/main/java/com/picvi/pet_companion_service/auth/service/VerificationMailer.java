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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/** Sends over SMTP when spring.mail.host is set; without it the code is only logged if pawstep.auth.log-codes is on (local dev), otherwise sending fails. */
@Slf4j
@Component
@RequiredArgsConstructor
public class VerificationMailer {
    private static final String TEMPLATE = "mail/verification-code.html";
    private static final String CODE_PLACEHOLDER = "{{code}}";

    private final ObjectProvider<JavaMailSender> sender;

    @Value("${pawstep.auth.mail-from:}")
    private String from;

    /** Only for local debugging: without a mail server the code is written to the log instead. */
    @Value("${pawstep.auth.log-codes:false}")
    private boolean logCodes;

    private volatile String htmlTemplate;

    public void sendCode(String to, String code) {
        JavaMailSender mailSender = this.sender.getIfAvailable();
        if (mailSender == null) {
            if (!this.logCodes) {
                throw new IllegalStateException("Mail is not configured (spring.mail.host), cannot send the verification code");
            }
            log.warn("Mail is not configured; verification code for {}: {}", to, code);
            return;
        }
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            if (!this.from.isBlank()) {
                helper.setFrom(this.from);
            }
            helper.setTo(to);
            helper.setSubject("Your curl up code: " + code);
            helper.setText(this.plainText(code), this.html(code));
        } catch (MessagingException e) {
            throw new MailPreparationException(e);
        }
        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.error("Could not send the verification code to {}", to, e);
            throw e;
        }
    }

    String html(String code) {
        return this.template().replace(CODE_PLACEHOLDER, code);
    }

    String plainText(String code) {
        return "Your curl up code: " + code + "\n\n"
                + "Type it in the app to finish creating your account. It works for 15 minutes, one time only.\n\n"
                + "Code expired? Open the app and tap \"Send a new code\".\n\n"
                + "Did not sign up for curl up? Ignore this email. Nothing is created until the code is entered.\n";
    }

    private String template() {
        String loaded = this.htmlTemplate;
        if (loaded == null) {
            try (var in = new ClassPathResource(TEMPLATE).getInputStream()) {
                loaded = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException("Cannot read " + TEMPLATE, e);
            }
            this.htmlTemplate = loaded;
        }
        return loaded;
    }
}
