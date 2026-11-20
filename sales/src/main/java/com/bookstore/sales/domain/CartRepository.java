package com.bookstore.sales.domain;

import com.bookstore.shared.kernel.identity.CustomerId;

import java.util.Optional;

/**
 * Stores and finds customers' carts. Guest carts are kept in the HTTP session and never reach this
 * repository.
 */
public interface CartRepository {

    /**
     * Finds a customer's cart.
     *
     * @param customerId the customer's id
     * @return the cart, or empty if the customer never had one
     */
    Optional<Cart> findByCustomerId(CustomerId customerId);

    /**
     * Stores a customer's cart, replacing all of its stored lines.
     *
     * @param cart a cart that belongs to a customer
     * @return the same cart
     * @throws IllegalArgumentException for a guest cart
     */
    Cart save(Cart cart);
}
