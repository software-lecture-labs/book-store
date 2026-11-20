package com.bookstore.sales.api;

import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.kernel.money.Money;

import java.time.Instant;

/**
 * One row of an order list.
 *
 * @param orderNumber   the order number
 * @param customerId    the customer who placed it
 * @param placedAt      when it was placed
 * @param status        the current status: {@code CREATED}, {@code PAID}, {@code SHIPPED}, {@code DELIVERED} or {@code CANCELLED}
 * @param paymentMethod how it is paid: {@code CREDIT_CARD} or {@code BANK_TRANSFER}
 * @param total         the amount to pay
 * @param totalQuantity the number of copies
 */
public record OrderSummary(String orderNumber, CustomerId customerId, Instant placedAt, String status,
                           String paymentMethod, Money total, int totalQuantity) {
}
