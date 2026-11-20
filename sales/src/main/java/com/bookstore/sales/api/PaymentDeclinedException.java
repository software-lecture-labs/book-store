package com.bookstore.sales.api;

import com.bookstore.shared.kernel.error.ConflictException;

/**
 * Signals that the payment for a new order was refused, so no order was placed. Maps to HTTP 409.
 */
public class PaymentDeclinedException extends ConflictException {

    /** Error code for a refused payment. */
    public static final String CODE = "payment.declined";

    private final String reason;

    /**
     * Creates the exception.
     *
     * @param reason why the payment was refused, in words the customer may see
     */
    public PaymentDeclinedException(String reason) {
        super(CODE, "Payment declined: " + reason);
        this.reason = reason;
    }

    /**
     * Returns why the payment was refused.
     *
     * @return the reason
     */
    public String reason() {
        return reason;
    }
}
