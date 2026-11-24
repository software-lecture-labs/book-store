package com.bookstore.sales.domain;

import com.bookstore.shared.kernel.event.DomainEvent;
import com.bookstore.shared.kernel.identity.CustomerId;

/**
 * Everything that can happen to an order (ADR-0007). The interface is {@code sealed}, so a
 * {@code switch} over order events is checked for completeness by the compiler.
 */
public sealed interface OrderEvent extends DomainEvent permits OrderPlaced, OrderStatusChanged {

    /**
     * Returns the number of the order the event is about.
     *
     * @return the order number
     */
    OrderNumber orderNumber();

    /**
     * Returns the customer the order belongs to.
     *
     * @return the customer's id
     */
    CustomerId customerId();
}
