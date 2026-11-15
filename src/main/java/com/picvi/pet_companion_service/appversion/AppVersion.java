package com.picvi.pet_companion_service.appversion;

import java.util.Comparator;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record AppVersion(int major, int minor, int patch) implements Comparable<AppVersion> {
    private static final Pattern FORMAT = Pattern.compile("(\\d{1,4})\\.(\\d{1,4})\\.(\\d{1,4})(\\+\\d+)?");
    private static final Comparator<AppVersion> ORDER = Comparator
            .comparingInt(AppVersion::major)
            .thenComparingInt(AppVersion::minor)
            .thenComparingInt(AppVersion::patch);

    public static Optional<AppVersion> parse(String value) {
        if (value == null) {
            return Optional.empty();
        }
        Matcher matcher = FORMAT.matcher(value.trim());
        if (!matcher.matches()) {
            return Optional.empty();
        }
        return Optional.of(new AppVersion(
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)),
                Integer.parseInt(matcher.group(3))));
    }

    @Override
    public int compareTo(AppVersion other) {
        return ORDER.compare(this, other);
    }
}
