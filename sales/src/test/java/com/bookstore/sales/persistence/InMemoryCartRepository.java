package com.bookstore.sales.persistence;

import com.bookstore.sales.domain.Cart;
import com.bookstore.sales.domain.CartRepository;
import com.bookstore.shared.kernel.identity.CustomerId;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Customers' carts keyed by customer id. Stores and returns copies, like the database. */
public class InMemoryCartRepository implements CartRepository {

    private final Map<CustomerId, Cart> byCustomerId = new ConcurrentHashMap<>();

    private static Cart copyOf(Cart cart) {
        return Cart.reconstitute(cart.customerId().orElseThrow(), cart.items());
    }

    @Override
    public Optional<Cart> findByCustomerId(CustomerId customerId) {
        return Optional.ofNullable(byCustomerId.get(customerId)).map(InMemoryCartRepository::copyOf);
    }

    @Override
    public Cart save(Cart cart) {
        CustomerId owner = cart.customerId().orElseThrow(() -> new IllegalArgumentException("guest carts are not stored"));
        byCustomerId.put(owner, copyOf(cart));
        return cart;
    }
}
