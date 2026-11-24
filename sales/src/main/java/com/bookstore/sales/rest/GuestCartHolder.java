package com.bookstore.sales.rest;

import com.bookstore.sales.api.GuestCart;

/**
 * Where a visitor's cart lives between requests (CRT-2, ADR-0020). On the server it is the HTTP
 * session ({@link SessionGuestCart}); the HTTP tests use a plain object.
 */
@FunctionalInterface
public interface GuestCartHolder {

    /**
     * Returns the visitor's cart; the cart use cases change it in place.
     *
     * @return the cart, empty on the first visit
     */
    GuestCart cart();
}
