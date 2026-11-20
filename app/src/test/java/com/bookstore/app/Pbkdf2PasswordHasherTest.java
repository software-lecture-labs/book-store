package com.bookstore.app;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class Pbkdf2PasswordHasherTest {

    private final Pbkdf2PasswordHasher hasher = new Pbkdf2PasswordHasher(1_000);

    @Test
    void theRightPasswordMatchesAndAWrongOneDoesNot() {
        String hash = hasher.hash("kitap2026");

        assertThat(hash).startsWith("pbkdf2$1000$").doesNotContain("kitap2026");
        assertThat(hasher.matches("kitap2026", hash)).isTrue();
        assertThat(hasher.matches("kitap2027", hash)).isFalse();
    }

    @Test
    void eachHashHasItsOwnSalt() {
        assertThat(hasher.hash("kitap2026")).isNotEqualTo(hasher.hash("kitap2026"));
    }

    @Test
    void foreignOrBrokenHashesNeverMatch() {
        assertThat(hasher.matches("kitap2026", "{hash}kitap2026")).isFalse();
        assertThat(hasher.matches("kitap2026", "pbkdf2$x$y$z")).isFalse();
        assertThat(hasher.matches("kitap2026", "pbkdf2$1000$!!!$???")).isFalse();
    }
}
