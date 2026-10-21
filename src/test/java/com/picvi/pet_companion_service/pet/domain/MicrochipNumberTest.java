package com.picvi.pet_companion_service.pet.domain;

import com.picvi.pet_companion_service.exception.pet.InvalidMicrochip;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MicrochipNumberTest {

    @Test
    void removesSpaces() {
        assertThat(MicrochipNumber.normalise("616 093 900 004 821")).isEqualTo("616093900004821");
    }

    @Test
    void blankOrMissingMeansNoChip() {
        assertThat(MicrochipNumber.normalise("   ")).isNull();
        assertThat(MicrochipNumber.normalise("")).isNull();
        assertThat(MicrochipNumber.normalise(null)).isNull();
    }

    @Test
    void rejectsWrongLength() {
        assertThatThrownBy(() -> MicrochipNumber.normalise("12345")).isInstanceOf(InvalidMicrochip.class);
    }

    @Test
    void rejectsLetters() {
        assertThatThrownBy(() -> MicrochipNumber.normalise("abcdefghijklmno")).isInstanceOf(InvalidMicrochip.class);
    }
}
