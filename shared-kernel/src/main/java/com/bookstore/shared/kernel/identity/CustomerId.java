package com.bookstore.shared.kernel.identity;

/**
 * The id of a registered customer. It lives in the shared kernel because other modules' domains
 * (a cart, an order, a review) refer to customers by id (ADR-0004).
 *
 * @param value the number the database assigned, greater than zero
 */
public record CustomerId(long value) implements Identifier {

    /** Rejects zero and negative values. */
    public CustomerId {
        Identifier.requireValid(value, "CustomerId");
    }

    /**
     * Returns the id with a value.
     *
     * @param value the value, greater than zero
     * @return the id
     */
    public static CustomerId of(long value) {
        return new CustomerId(value);
    }

    @Override
    public String toString() {
        return Long.toString(value);
    }
}
