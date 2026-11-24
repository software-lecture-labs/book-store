package com.bookstore.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bookstore.shared.kernel.error.ValidationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class IsbnTest {

    @ParameterizedTest
    @ValueSource(strings = {"9789750719387", "978-975-07-1938-7", " 978 9750719387 "})
    void parsesThirteenDigitsWithOrWithoutSeparators(String text) {
        assertThat(Isbn.parse(text).value()).isEqualTo("9789750719387");
    }

    @Test
    void convertsIsbn10ToIsbn13() {
        assertThat(Isbn.parse("0-306-40615-2").value()).isEqualTo("9780306406157");
    }

    @Test
    void acceptsXAsTheLastIsbn10Digit() {
        assertThat(Isbn.parse("080442957x").value()).isEqualTo("9780804429573");
    }

    @ParameterizedTest
    @ValueSource(strings = {"9789750719388", "9771234567897", "978975071938", "97897507193877", "0306406153", "abc"})
    void rejectsInvalidNumbers(String text) {
        assertThatThrownBy(() -> Isbn.parse(text)).isInstanceOf(ValidationException.class);
    }

    @Test
    void formatsWithThePrefixSeparated() {
        assertThat(Isbn.parse("9789750719387").formatted()).isEqualTo("978-9750719387");
    }
}
