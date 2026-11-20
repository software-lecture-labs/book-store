package com.bookstore.sales.api;

import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.kernel.validation.Guard;

/**
 * The checkout form (ORD-2).
 *
 * @param customerId     the signed-in customer
 * @param addressId      the chosen saved address
 * @param paymentMethod  the chosen payment method: {@code CREDIT_CARD}, {@code BANK_TRANSFER} or
 *                       {@code CASH_ON_DELIVERY}
 * @param idempotencyKey the client's key for this order, so a repeat does not place a second one
 *                       (ADR-0022); {@code null} without a key
 */
public record PlaceOrderCommand(CustomerId customerId, long addressId, String paymentMethod, String idempotencyKey) {

    /** Rejects a missing payment method. */
    public PlaceOrderCommand {
        Guard.requireNonNull(paymentMethod, "paymentMethod");
    }

    /**
     * A form without an idempotency key.
     *
     * @param customerId    the signed-in customer
     * @param addressId     the chosen saved address
     * @param paymentMethod the chosen payment method
     */
    public PlaceOrderCommand(CustomerId customerId, long addressId, String paymentMethod) {
        this(customerId, addressId, paymentMethod, null);
    }
}
