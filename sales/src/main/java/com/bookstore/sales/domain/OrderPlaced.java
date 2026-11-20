package com.bookstore.sales.domain;

import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.kernel.money.Money;

import java.time.Instant;
import java.util.Objects;

/**
 * A customer has placed an order.
 *
 * @param orderNumber the new order's number
 * @param customerId  who placed it
 * @param total       the amount to pay, including shipping
 * @param occurredAt  when it was placed
 */
public record OrderPlaced(OrderNumber orderNumber, CustomerId customerId, Money total, Instant occurredAt)
        implements OrderEvent {

    /** Rejects missing parts. */
    public OrderPlaced {
        Objects.requireNonNull(orderNumber, "orderNumber");
        Objects.requireNonNull(total, "total");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
