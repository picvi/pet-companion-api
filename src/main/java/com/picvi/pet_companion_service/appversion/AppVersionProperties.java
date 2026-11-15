package com.picvi.pet_companion_service.appversion;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pawstep.app-version")
public record AppVersionProperties(boolean enabled, Store ios, Store android) {

    public record Store(String minimum, String latest, String storeUrl) {
    }
}
