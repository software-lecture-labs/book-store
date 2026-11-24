package com.bookstore.shared.kernel.identity;

import com.bookstore.shared.kernel.error.ValidationException;

import java.io.Serializable;

/**
 * The typed id of an aggregate or entity (ADR-0004). Each id is its own {@code record}, so that a
 * {@code BookId} cannot be passed where a {@code CustomerId} is expected.
 *
 * <p>The database assigns the number; ids are therefore always greater than zero. Ids are
 * serializable because a guest's cart, which refers to books by id, is kept in the HTTP session.
 */
public interface Identifier extends Serializable {

    /**
     * Returns the number the database assigned.
     *
     * @return the id's value, greater than zero
     */
    long value();

    /**
     * Checks an id's value. Called from each id record's constructor.
     *
     * @param value the value
     * @param type  the id type, for the error message
     * @return the value
     * @throws ValidationException if the value is zero or negative
     */
    static long requireValid(long value, String type) {
        if (value <= 0) {
            throw new ValidationException(type, "must be greater than zero, was " + value);
        }
        return value;
    }
}
