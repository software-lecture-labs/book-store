package com.bookstore.app.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ClientHeaderFilterTest {

    @Test
    void aCookieSessionThatChangesStateNeedsTheClientHeader() {
        assertThat(ClientHeaderFilter.rejects("POST", false, true, false)).isTrue();
        assertThat(ClientHeaderFilter.rejects("PUT", false, true, false)).isTrue();
        assertThat(ClientHeaderFilter.rejects("DELETE", false, true, false)).isTrue();
        assertThat(ClientHeaderFilter.rejects("POST", false, true, true)).isFalse();
    }

    @Test
    void readingBasicAndCookielessRequestsAreNotChecked() {
        assertThat(ClientHeaderFilter.rejects("GET", false, true, false)).isFalse();
        assertThat(ClientHeaderFilter.rejects("HEAD", false, true, false)).isFalse();
        assertThat(ClientHeaderFilter.rejects("POST", true, true, false)).isFalse();
        assertThat(ClientHeaderFilter.rejects("POST", false, false, false)).isFalse();
    }
}
