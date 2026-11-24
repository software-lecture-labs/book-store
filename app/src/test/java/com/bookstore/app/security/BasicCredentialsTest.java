package com.bookstore.app.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;

class BasicCredentialsTest {

    @Test
    void readsTheUserNameAndAPasswordThatMayContainColons() {
        String header = "basic " + Base64.getEncoder().encodeToString("ayşe@example.com:a:b".getBytes(StandardCharsets.UTF_8));

        assertThat(BasicCredentials.parse(header)).hasValueSatisfying(credential -> {
            assertThat(credential.getCaller()).isEqualTo("ayşe@example.com");
            assertThat(credential.getPasswordAsString()).isEqualTo("a:b");
        });
    }

    @Test
    void rejectsOtherSchemesBadBase64AndMissingParts() {
        assertThat(BasicCredentials.parse(null)).isEmpty();
        assertThat(BasicCredentials.parse("Bearer abc")).isEmpty();
        assertThat(BasicCredentials.parse("Basic !!!")).isEmpty();
        assertThat(BasicCredentials.parse("Basic " + Base64.getEncoder().encodeToString("nocolon".getBytes()))).isEmpty();
        assertThat(BasicCredentials.parse("Basic " + Base64.getEncoder().encodeToString(":secret".getBytes()))).isEmpty();
    }
}
