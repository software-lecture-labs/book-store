package com.bookstore.sales.rest;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * The request bodies of the cart and order endpoints. Bean Validation checks only their format
 * (ADR-0009); whether a book is sold or in stock is the domain's business.
 */
public final class SalesRequests {

    private SalesRequests() {
    }

    /**
     * {@code POST /api/v1/cart/items}.
     *
     * @param bookId   the book
     * @param quantity how many copies to add
     */
    public record AddToCartRequest(@NotNull @Positive Long bookId, @Min(1) @Max(10) int quantity) {
    }

    /**
     * {@code PUT /api/v1/cart/items/{bookId}}.
     *
     * @param quantity the new number of copies; zero removes the book
     */
    public record QuantityRequest(@Min(0) @Max(10) int quantity) {
    }

    /**
     * {@code POST /api/v1/orders}.
     *
     * @param addressId     one of the customer's saved addresses
     * @param paymentMethod {@code CREDIT_CARD}, {@code BANK_TRANSFER} or {@code CASH_ON_DELIVERY}
     */
    public record PlaceOrderRequest(@NotNull @Positive Long addressId, @NotBlank String paymentMethod) {
    }

    /**
     * {@code PUT /api/v1/admin/orders/{number}/status}.
     *
     * @param status the new status, for example {@code SHIPPED}
     */
    public record StatusChangeRequest(@NotBlank String status) {
    }
}
