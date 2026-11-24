package com.bookstore.sales.rest;

import com.bookstore.sales.api.GuestCart;

import java.io.Serial;
import java.io.Serializable;

import jakarta.enterprise.context.SessionScoped;

/**
 * Keeps a visitor's cart in the HTTP session (CRT-2, ADR-0020). The container makes one per
 * session, the first time a visitor uses the cart; signing out ends the session and the cart.
 */
@SessionScoped
public class SessionGuestCart implements GuestCartHolder, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final GuestCart cart = new GuestCart();

    @Override
    public GuestCart cart() {
        return cart;
    }
}
