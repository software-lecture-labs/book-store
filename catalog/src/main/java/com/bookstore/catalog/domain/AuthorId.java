package com.bookstore.catalog.domain;

import com.bookstore.shared.kernel.identity.Identifier;

/**
 * The id of an author (ADR-0004).
 *
 * @param value the number the database assigned, greater than zero
 */
public record AuthorId(long value) implements Identifier {

    /** Rejects zero and negative values. */
    public AuthorId {
        Identifier.requireValid(value, "AuthorId");
    }

    /**
     * Returns the id with a value.
     *
     * @param value the value, greater than zero
     * @return the id
     */
    public static AuthorId of(long value) {
        return new AuthorId(value);
    }

    @Override
    public String toString() {
        return Long.toString(value);
    }
}
