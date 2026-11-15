package com.picvi.pet_companion_service.appversion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class AppVersionTest {

    @Test
    void parsesPlainAndBuildSuffixedVersions() {
        assertThat(AppVersion.parse("1.2.3")).contains(new AppVersion(1, 2, 3));
        assertThat(AppVersion.parse("1.2.3+45")).contains(new AppVersion(1, 2, 3));
        assertThat(AppVersion.parse(" 10.0.1 ")).contains(new AppVersion(10, 0, 1));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"1.2", "1.2.3.4", "v1.2.3", "1.2.x", "1.2.3-beta", "-1.0.0"})
    void rejectsAnythingElse(String value) {
        assertThat(AppVersion.parse(value)).isEmpty();
    }

    @Test
    void comparesNumericallyNotAsText() {
        assertThat(new AppVersion(1, 10, 0)).isGreaterThan(new AppVersion(1, 9, 9));
        assertThat(new AppVersion(2, 0, 0)).isGreaterThan(new AppVersion(1, 99, 99));
        assertThat(new AppVersion(1, 0, 0)).isEqualByComparingTo(new AppVersion(1, 0, 0));
    }
}
