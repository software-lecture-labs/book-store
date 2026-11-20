package com.bookstore.app.security;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import jakarta.security.enterprise.credential.UsernamePasswordCredential;

/** Reads the user name and password of an {@code Authorization: Basic} header (RFC 7617). */
public final class BasicCredentials {

    private static final String SCHEME = "Basic ";

    private BasicCredentials() {
    }

    /**
     * Reads a header.
     *
     * @param authorization the value of the {@code Authorization} header
     * @return the credential; empty if the header is not well-formed Basic
     */
    public static Optional<UsernamePasswordCredential> parse(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, SCHEME, 0, SCHEME.length())) {
            return Optional.empty();
        }
        String decoded;
        try {
            decoded = new String(Base64.getDecoder().decode(authorization.substring(SCHEME.length()).trim()),
                    StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        int colon = decoded.indexOf(':');
        if (colon <= 0) {
            return Optional.empty();
        }
        return Optional.of(new UsernamePasswordCredential(decoded.substring(0, colon), decoded.substring(colon + 1)));
    }
}
