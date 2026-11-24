package com.bookstore.sales.domain;

import com.bookstore.shared.kernel.money.Money;

/**
 * Decides the shipping cost of an order (Strategy pattern). The store's rule can be swapped without
 * touching {@link Order}.
 */
@FunctionalInterface
public interface ShippingPolicy {

    /**
     * Returns the shipping cost for an order.
     *
     * @param subtotal the price of the books, before shipping
     * @return the shipping cost, zero or greater
     */
    Money costFor(Money subtotal);

    /**
     * Returns the store's standard rule: free from 500.00 TRY, otherwise 49.90 TRY.
     *
     * @return the standard policy
     */
    static ShippingPolicy standard() {
        return new FreeShippingThreshold(Money.of("500.00"), Money.of("49.90"));
    }

    /**
     * Returns a rule under which shipping is always free.
     *
     * @return the policy
     */
    static ShippingPolicy alwaysFree() {
        return subtotal -> Money.ZERO;
    }
}
