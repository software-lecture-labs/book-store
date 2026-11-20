package com.bookstore.sales.domain;

/**
 * How the customer chose to pay. Payment is simulated; no card data is ever stored.
 */
public enum PaymentMethod {

    /** Credit or debit card. */
    CREDIT_CARD,

    /** Bank transfer (EFT / havale). */
    BANK_TRANSFER,

    /** Cash or card to the courier. */
    CASH_ON_DELIVERY
}
