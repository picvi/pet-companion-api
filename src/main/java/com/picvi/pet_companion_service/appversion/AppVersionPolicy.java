package com.picvi.pet_companion_service.appversion;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
@EnableConfigurationProperties(AppVersionProperties.class)
public class AppVersionPolicy {
    private final AppVersionProperties properties;
    private final Map<AppPlatform, AppVersion> minimums = new EnumMap<>(AppPlatform.class);

    public AppVersionPolicy(AppVersionProperties properties) {
        this.properties = properties;
        this.minimums.put(AppPlatform.IOS, requireVersion(properties.ios().minimum()));
        this.minimums.put(AppPlatform.ANDROID, requireVersion(properties.android().minimum()));
        requireVersion(properties.ios().latest());
        requireVersion(properties.android().latest());
    }

    public boolean isEnforced() {
        return this.properties.enabled();
    }

    public boolean isSupported(AppPlatform platform, AppVersion version) {
        return version.compareTo(this.minimums.get(platform)) >= 0;
    }

    public VersionPolicyResponse responseFor(AppPlatform platform) {
        AppVersionProperties.Store store = platform == AppPlatform.IOS ? this.properties.ios() : this.properties.android();
        return new VersionPolicyResponse(store.minimum(), store.latest(), store.storeUrl());
    }

    private static AppVersion requireVersion(String value) {
        return AppVersion.parse(value)
                .orElseThrow(() -> new IllegalStateException("pawstep.app-version needs major.minor.patch, got: " + value));
    }
}
