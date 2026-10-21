package com.picvi.pet_companion_service.nutrition.digest;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.List;

/** Sends over SMTP when spring.mail.host is set; without it the digest is skipped and retried the next day. */
@Component
@RequiredArgsConstructor
public class SmtpDigestMailer implements DigestMailer {
    private final ObjectProvider<JavaMailSender> sender;

    @Override
    public boolean isConfigured() {
        return this.sender.getIfAvailable() != null;
    }

    @Override
    public void send(List<String> to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to.toArray(String[]::new));
        message.setSubject(subject);
        message.setText(body);
        this.sender.getObject().send(message);
    }
}
