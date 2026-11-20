package com.bookstore.sales.domain;

import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.kernel.validation.Guard;

import java.time.Instant;
import java.util.Objects;

/**
 * A request to place an order that came with an {@code Idempotency-Key} (ADR-0022): the key, what
 * was asked for, and the order it led to. A repeat of the same request gets that order back
 * instead of a second one.
 *
 * @param customerId  who sent it
 * @param key         the client's key, unique per customer
 * @param fingerprint what was asked for: the address and the payment method
 * @param orderNumber the order it led to
 * @param receivedAt  when it was first received
 */
public record OrderRequest(CustomerId customerId, String key, String fingerprint, OrderNumber orderNumber,
                           Instant receivedAt) {

    /** The longest key accepted. */
    public static final int MAX_KEY_LENGTH = 100;

    /** Rejects missing parts and keys that are blank or too long. */
    public OrderRequest {
        Objects.requireNonNull(customerId, "customerId");
        key = Guard.requireText(key, "idempotencyKey", MAX_KEY_LENGTH);
        Objects.requireNonNull(fingerprint, "fingerprint");
        Objects.requireNonNull(orderNumber, "orderNumber");
        Objects.requireNonNull(receivedAt, "receivedAt");
    }

    /**
     * Describes what an order request asked for, so a reused key with other contents is noticed.
     *
     * @param addressId     the delivery address
     * @param paymentMethod the payment method
     * @return the fingerprint
     */
    public static String fingerprintOf(long addressId, PaymentMethod paymentMethod) {
        return addressId + ":" + paymentMethod.name();
    }

    /**
     * Tells whether a repeat asks for the same thing.
     *
     * @param otherFingerprint the repeat's fingerprint
     * @return {@code true} if it does
     */
    public boolean sameAs(String otherFingerprint) {
        return fingerprint.equals(otherFingerprint);
    }
}
