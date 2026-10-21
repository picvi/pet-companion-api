package com.picvi.pet_companion_service.nutrition.digest;

import java.util.List;

public interface DigestMailer {

    boolean isConfigured();

    void send(List<String> to, String subject, String body);
}
