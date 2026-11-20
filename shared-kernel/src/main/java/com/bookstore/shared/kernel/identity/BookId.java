package com.bookstore.shared.kernel.identity;

/**
 * The id of a book in the catalog. It lives in the shared kernel because other modules' domains
 * (a cart line, an order line, a review) refer to books by id (ADR-0004).
 *
 * @param value the number the database assigned, greater than zero
 */
public record BookId(long value) implements Identifier {

    /** Rejects zero and negative values. */
    public BookId {
        Identifier.requireValid(value, "BookId");
    }

    /**
     * Returns the id with a value.
     *
     * @param value the value, greater than zero
     * @return the id
     */
    public static BookId of(long value) {
        return new BookId(value);
    }

    @Override
    public String toString() {
        return Long.toString(value);
    }
}
