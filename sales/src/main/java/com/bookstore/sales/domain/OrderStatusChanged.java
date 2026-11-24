package com.bookstore.sales.domain;

import com.bookstore.shared.kernel.identity.CustomerId;

import java.time.Instant;
import java.util.Objects;

/**
 * An order has moved from one status to another.
 *
 * @param orderNumber the order's number
 * @param customerId  the order's owner
 * @param from        the previous status
 * @param to          the new status
 * @param occurredAt  when the change happened
 */
public record OrderStatusChanged(
        OrderNumber orderNumber, CustomerId customerId, OrderStatus from, OrderStatus to, Instant occurredAt)
        implements OrderEvent {

    /** Rejects missing parts. */
    public OrderStatusChanged {
        Objects.requireNonNull(orderNumber, "orderNumber");
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
