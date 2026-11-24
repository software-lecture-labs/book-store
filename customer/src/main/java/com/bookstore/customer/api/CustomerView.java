package com.bookstore.customer.api;

import com.bookstore.shared.kernel.address.Address;
import com.bookstore.shared.kernel.identity.CustomerId;

import java.time.Instant;
import java.util.List;

/**
 * A customer's profile page. The password hash is deliberately left out.
 *
 * @param id           the customer's id
 * @param email        the e-mail address
 * @param firstName    the first name
 * @param lastName     the last name
 * @param role         the role: {@code CUSTOMER} or {@code ADMIN}
 * @param registeredAt when the customer registered
 * @param addresses    the saved addresses
 */
public record CustomerView(CustomerId id, String email, String firstName, String lastName, String role,
                           Instant registeredAt, List<AddressView> addresses) {

    /** Copies the addresses. */
    public CustomerView {
        addresses = List.copyOf(addresses);
    }

    /**
     * A saved address.
     *
     * @param id        the address id, used to pick it at checkout
     * @param label     the customer's name for it
     * @param address   the address
     * @param isDefault whether it is pre-selected at checkout
     */
    public record AddressView(long id, String label, Address address, boolean isDefault) {
    }
}
