package com.bookstore.shared.web;

import com.bookstore.shared.kernel.identity.CustomerId;

import java.util.Objects;

/**
 * Who sent a request, as the server's authentication established it (ADR-0020).
 *
 * @param customerId the signed-in customer
 * @param role       {@code CUSTOMER} or {@code ADMIN}
 */
public record Caller(CustomerId customerId, String role) {

    /** The role of every registered customer. */
    public static final String CUSTOMER = "CUSTOMER";

    /** The role that may use {@code /api/v1/admin/...}. */
    public static final String ADMIN = "ADMIN";

    /** Rejects missing parts. */
    public Caller {
        Objects.requireNonNull(customerId, "customerId");
        Objects.requireNonNull(role, "role");
    }

    /** @return {@code true} for an administrator */
    public boolean isAdmin() {
        return ADMIN.equals(role);
    }
}
