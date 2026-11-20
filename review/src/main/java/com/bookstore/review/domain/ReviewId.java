package com.bookstore.review.domain;

import com.bookstore.shared.kernel.identity.Identifier;

/**
 * The id of a review (ADR-0004).
 *
 * @param value the number the database assigned, greater than zero
 */
public record ReviewId(long value) implements Identifier {

    /** Rejects zero and negative values. */
    public ReviewId {
        Identifier.requireValid(value, "ReviewId");
    }

    /**
     * Returns the id with a value.
     *
     * @param value the value, greater than zero
     * @return the id
     */
    public static ReviewId of(long value) {
        return new ReviewId(value);
    }

    @Override
    public String toString() {
        return Long.toString(value);
    }
}
