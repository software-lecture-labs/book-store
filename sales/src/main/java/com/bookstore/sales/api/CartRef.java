package com.bookstore.sales.api;

import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.kernel.validation.Guard;

/**
 * Which cart an operation is about: a visitor's {@link GuestCart} or a signed-in customer's
 * stored cart.
 */
public sealed interface CartRef {

    /**
     * Refers to a visitor's cart.
     *
     * @param cart the cart the web layer keeps between requests
     * @return the reference
     */
    static CartRef guest(GuestCart cart) {
        return new Guest(cart);
    }

    /**
     * Refers to a customer's stored cart.
     *
     * @param customerId the signed-in customer
     * @return the reference
     */
    static CartRef customer(CustomerId customerId) {
        return new Customer(customerId);
    }

    /**
     * A visitor's cart, changed in place.
     *
     * @param cart the cart
     */
    record Guest(GuestCart cart) implements CartRef {

        /** Rejects a missing cart. */
        public Guest {
            Guard.requireNonNull(cart, "cart");
        }
    }

    /**
     * A customer's stored cart.
     *
     * @param customerId the customer
     */
    record Customer(CustomerId customerId) implements CartRef {

        /** Rejects a missing customer. */
        public Customer {
            Guard.requireNonNull(customerId, "customerId");
        }
    }
}
