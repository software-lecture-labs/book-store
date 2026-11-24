package com.bookstore.customer.domain;

import com.bookstore.shared.kernel.identity.Identifier;

/**
 * The id of an address saved in a customer's address book (ADR-0004).
 *
 * @param value the number the database assigned, greater than zero
 */
public record AddressId(long value) implements Identifier {

    /** Rejects zero and negative values. */
    public AddressId {
        Identifier.requireValid(value, "AddressId");
    }

    /**
     * Returns the id with a value.
     *
     * @param value the value, greater than zero
     * @return the id
     */
    public static AddressId of(long value) {
        return new AddressId(value);
    }

    @Override
    public String toString() {
        return Long.toString(value);
    }
}
