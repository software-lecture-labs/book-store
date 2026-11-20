package com.bookstore.sales.domain;

import com.bookstore.shared.kernel.error.ConflictException;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * The steps of an order's life (State pattern).
 *
 * <pre>
 * CREATED ──► PAID ──► SHIPPED ──► DELIVERED
 *    │          │
 *    └──────────┴──► CANCELLED
 * </pre>
 */
public enum OrderStatus {

    /** Placed, waiting for payment. */
    CREATED,

    /** Paid, waiting to be shipped. */
    PAID,

    /** Handed to the courier. */
    SHIPPED,

    /** Received by the customer. Final. */
    DELIVERED,

    /** Cancelled; stock has been returned. Final. */
    CANCELLED;

    /** Error code for a forbidden status change. */
    public static final String ILLEGAL_TRANSITION = "order.illegal-transition";

    /**
     * Returns the statuses an order in this status may move to.
     *
     * @return an unmodifiable set, empty for final statuses
     */
    public Set<OrderStatus> allowedNext() {
        Set<OrderStatus> next = switch (this) {
            case CREATED -> EnumSet.of(PAID, CANCELLED);
            case PAID -> EnumSet.of(SHIPPED, CANCELLED);
            case SHIPPED -> EnumSet.of(DELIVERED);
            case DELIVERED, CANCELLED -> EnumSet.noneOf(OrderStatus.class);
        };
        return Collections.unmodifiableSet(next);
    }

    /**
     * Tells whether an order in this status may move to another.
     *
     * @param target the wanted status
     * @return {@code true} if the change is allowed
     */
    public boolean canMoveTo(OrderStatus target) {
        return allowedNext().contains(target);
    }

    /**
     * Tells whether the order can no longer change.
     *
     * @return {@code true} for {@link #DELIVERED} and {@link #CANCELLED}
     */
    public boolean isFinal() {
        return allowedNext().isEmpty();
    }

    /**
     * Throws if an order in this status may not move to another.
     *
     * @param target the wanted status
     * @throws ConflictException if the change is not allowed
     */
    public void requireCanMoveTo(OrderStatus target) {
        if (!canMoveTo(target)) {
            throw new ConflictException(ILLEGAL_TRANSITION, "An order cannot move from " + this + " to " + target);
        }
    }
}
