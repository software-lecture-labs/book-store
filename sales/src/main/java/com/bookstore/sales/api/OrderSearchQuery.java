package com.bookstore.sales.api;

import com.bookstore.shared.kernel.validation.Guard;

import java.time.LocalDate;
import java.util.Locale;

/**
 * Filters for the administrator's order list.
 *
 * @param status      the status, or {@code null} for every status
 * @param orderNumber a case-insensitive fragment of the order number, or {@code null}
 * @param from        the first order date, inclusive, or {@code null}
 * @param to          the last order date, inclusive, or {@code null}
 */
public record OrderSearchQuery(String status, String orderNumber, LocalDate from, LocalDate to) {

    /** Validates and normalizes optional filters. */
    public OrderSearchQuery {
        status = Guard.optionalText(status, "status", 20);
        orderNumber = Guard.optionalText(orderNumber, "orderNumber", 14);
        if (orderNumber != null) {
            orderNumber = orderNumber.toUpperCase(Locale.ROOT);
        }
        Guard.require(from == null || supportedDate(from), "from", "year must be between 1 and 9999");
        Guard.require(to == null || supportedDate(to), "to", "year must be between 1 and 9999");
        if (from != null && to != null) {
            Guard.require(!to.isBefore(from), "to", "must not be before from");
        }
    }

    private static boolean supportedDate(LocalDate date) {
        return date.getYear() >= 1 && date.getYear() <= 9999;
    }
}
