package com.bookstore.shared.kernel.text;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

class TextNormalizerTest {

    @ParameterizedTest
    @CsvSource({
            "Çalıkuşu,       calikusu",
            "ÇALIKUŞU,       calikusu",
            "İstanbul,       istanbul",
            "ISTANBUL,       istanbul",
            "Öğretmen,       ogretmen",
            "Les Misérables, les miserables",
            "IT,             it",
    })
    void ignoresCaseAccentsAndTurkishLetters(String input, String expected) {
        assertThat(TextNormalizer.searchKey(input)).isEqualTo(expected);
    }

    @Test
    void collapsesAndTrimsWhitespace() {
        assertThat(TextNormalizer.searchKey("  Suç   ve\tCeza  ")).isEqualTo("suc ve ceza");
    }

    @Test
    void containsComparesSearchKeys() {
        assertThat(TextNormalizer.containsIgnoringCase("Kürk Mantolu Madonna", "MANTOLU")).isTrue();
        assertThat(TextNormalizer.containsIgnoringCase("Kürk Mantolu Madonna", "kurk")).isTrue();
        assertThat(TextNormalizer.containsIgnoringCase("Kürk Mantolu Madonna", "palto")).isFalse();
    }
}
