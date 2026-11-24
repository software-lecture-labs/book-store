package com.bookstore.shared.web;

import java.util.Optional;

/**
 * Checks the user name and password of an HTTP Basic request in the HTTP tests, where
 * {@link BasicAuthenticationFilter} stands in for the server's Jakarta Security (ADR-0020).
 */
@FunctionalInterface
public interface Authenticator {

    /** An authenticator that knows nobody, for APIs without protected endpoints. */
    Authenticator NOBODY = (username, password) -> Optional.empty();

    /**
     * Checks credentials.
     *
     * @param username the e-mail address
     * @param password the password
     * @return the caller, or empty if the credentials are wrong or the account is disabled
     */
    Optional<Caller> authenticate(String username, String password);
}
