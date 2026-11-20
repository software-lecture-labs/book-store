package com.bookstore.customer.api;

import com.bookstore.shared.kernel.identity.CustomerId;

import java.time.Instant;

/**
 * An account in the administrator's list (ADM-6).
 *
 * @param id           the account's id
 * @param email        the e-mail address
 * @param firstName    the first name
 * @param lastName     the last name
 * @param role         {@code CUSTOMER} or {@code ADMIN}
 * @param active       whether the account may sign in
 * @param registeredAt when the account was opened
 */
public record CustomerSummary(CustomerId id, String email, String firstName, String lastName, String role,
                              boolean active, Instant registeredAt) {
}
