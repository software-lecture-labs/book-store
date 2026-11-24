package com.bookstore.app;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BcryptPasswordHasherTest {

    private final BcryptPasswordHasher hasher = new BcryptPasswordHasher(4);

    @Test
    void theRightPasswordMatchesAndAWrongOneDoesNot() {
        String hash = hasher.hash("kitap2026");

        assertThat(hash).startsWith("$2y$04$").doesNotContain("kitap2026");
        assertThat(hasher.matches("kitap2026", hash)).isTrue();
        assertThat(hasher.matches("kitap2027", hash)).isFalse();
    }

    @Test
    void eachHashHasItsOwnSalt() {
        assertThat(hasher.hash("kitap2026")).isNotEqualTo(hasher.hash("kitap2026"));
    }

    @Test
    void longPasswordsAreNotCutAt72Bytes() {
        String prefix = "a".repeat(72);
        String hash = hasher.hash(prefix + "1");

        assertThat(hasher.matches(prefix + "1", hash)).isTrue();
        assertThat(hasher.matches(prefix + "2", hash)).isFalse();
    }

    @Test
    void hashesFromBeforePhase7StillMatch() {
        String old = new Pbkdf2PasswordHasher(1_000).hash("kitap2026");

        assertThat(hasher.matches("kitap2026", old)).isTrue();
        assertThat(hasher.matches("wrong", old)).isFalse();
    }

    @Test
    void foreignHashesNeverMatch() {
        assertThat(hasher.matches("kitap2026", "{hash}kitap2026")).isFalse();
    }
}
