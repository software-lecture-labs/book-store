package com.bookstore.customer.api;

import com.bookstore.shared.kernel.identity.CustomerId;

/**
 * What the security layer needs to sign a customer in. The security layer checks the password
 * against {@link #passwordHash()} itself.
 *
 * @param customerId   the customer's id
 * @param email        the e-mail address used as the user name
 * @param passwordHash the stored password hash
 * @param role         the role: {@code CUSTOMER} or {@code ADMIN}
 * @param active       whether the customer may sign in (ACC-7)
 */
public record LoginAccount(CustomerId customerId, String email, String passwordHash, String role, boolean active) {

    /** Hides the password hash when the account is logged. */
    @Override
    public String toString() {
        return "LoginAccount[customerId=" + customerId + ", email=" + email + ", role=" + role + ", active="
                + active + "]";
    }
}
