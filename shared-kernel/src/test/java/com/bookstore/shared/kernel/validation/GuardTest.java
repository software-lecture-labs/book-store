package com.bookstore.shared.kernel.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bookstore.shared.kernel.error.ValidationException;

import java.time.Year;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class GuardTest {

    @Test
    void requireTextTrimsAndRejectsBlankOrTooLong() {
        assertThat(Guard.requireText("  Dune  ", "title", 10)).isEqualTo("Dune");

        assertThatThrownBy(() -> Guard.requireText("   ", "title", 10))
                .isInstanceOf(ValidationException.class)
                .satisfies(e -> assertThat(((ValidationException) e).violations())
                        .singleElement()
                        .satisfies(v -> assertThat(v.field()).isEqualTo("title")));
        assertThatThrownBy(() -> Guard.requireText("x".repeat(11), "title", 10))
                .hasMessageContaining("at most 10");
        assertThatThrownBy(() -> Guard.requireText(null, "title", 10))
                .hasMessageContaining("required");
    }

    @Test
    void optionalTextTurnsNullIntoEmptyString() {
        assertThat(Guard.optionalText(null, "description", 5)).isEmpty();
        assertThat(Guard.optionalText("  hi ", "description", 5)).isEqualTo("hi");
    }

    @Test
    void requireBetweenWorksForAnyComparableType() {
        assertThat(Guard.requireBetween(3, 1, 5, "rating")).isEqualTo(3);
        assertThat(Guard.requireBetween(Year.of(1999), Year.of(1450), Year.of(2100), "year"))
                .isEqualTo(Year.of(1999));

        assertThatThrownBy(() -> Guard.requireBetween(6, 1, 5, "rating"))
                .hasMessageContaining("between 1 and 5");
    }

    @Test
    void numericChecks() {
        assertThat(Guard.requirePositive(1, "n")).isEqualTo(1);
        assertThat(Guard.requireNonNegative(0, "n")).isZero();

        assertThatThrownBy(() -> Guard.requirePositive(0, "n")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> Guard.requireNonNegative(-1, "n")).isInstanceOf(ValidationException.class);
    }

    @Test
    void requireMatchesChecksTheWholeText() {
        Pattern digits = Pattern.compile("\\d+");

        assertThat(Guard.requireMatches("123", digits, "code", "digits only")).isEqualTo("123");
        assertThatThrownBy(() -> Guard.requireMatches("12a", digits, "code", "digits only"))
                .hasMessageContaining("digits only");
    }

    @Test
    void requireNotEmptyRejectsEmptyCollectionsAndNullEntries() {
        assertThat(Guard.requireNotEmpty(List.of("a"), "authors")).containsExactly("a");

        assertThatThrownBy(() -> Guard.requireNotEmpty(List.of(), "authors"))
                .hasMessageContaining("must not be empty");
        assertThatThrownBy(() -> Guard.requireNotEmpty(Arrays.asList("a", null), "authors"))
                .hasMessageContaining("empty entries");
    }
}
