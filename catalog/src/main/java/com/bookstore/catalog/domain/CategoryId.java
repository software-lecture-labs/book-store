package com.bookstore.catalog.domain;

import com.bookstore.shared.kernel.identity.Identifier;

/**
 * The id of a category (ADR-0004).
 *
 * @param value the number the database assigned, greater than zero
 */
public record CategoryId(long value) implements Identifier {

    /** Rejects zero and negative values. */
    public CategoryId {
        Identifier.requireValid(value, "CategoryId");
    }

    /**
     * Returns the id with a value.
     *
     * @param value the value, greater than zero
     * @return the id
     */
    public static CategoryId of(long value) {
        return new CategoryId(value);
    }

    @Override
    public String toString() {
        return Long.toString(value);
    }
}
