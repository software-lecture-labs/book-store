package com.bookstore.sales.domain;

import com.bookstore.shared.kernel.identity.Identifier;

/**
 * The id of an order (ADR-0004). Customers see the {@link OrderNumber} instead.
 *
 * @param value the number the database assigned, greater than zero
 */
public record OrderId(long value) implements Identifier {

    /** Rejects zero and negative values. */
    public OrderId {
        Identifier.requireValid(value, "OrderId");
    }

    /**
     * Returns the id with a value.
     *
     * @param value the value, greater than zero
     * @return the id
     */
    public static OrderId of(long value) {
        return new OrderId(value);
    }

    @Override
    public String toString() {
        return Long.toString(value);
    }
}
