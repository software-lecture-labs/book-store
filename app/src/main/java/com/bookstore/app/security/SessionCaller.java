package com.bookstore.app.security;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Set;

/**
 * Who signed in with {@code POST /api/v1/session}, as kept in the HTTP session (ADR-0020).
 *
 * @param name   the caller's name: the customer's id
 * @param groups the caller's groups: the role
 */
public record SessionCaller(String name, Set<String> groups) implements Serializable {

    /** The session attribute that holds the caller. */
    public static final String ATTRIBUTE = SessionCaller.class.getName();

    @Serial
    private static final long serialVersionUID = 1L;

    /** Rejects missing parts and copies the groups. */
    public SessionCaller {
        Objects.requireNonNull(name, "name");
        groups = Set.copyOf(groups);
    }
}
