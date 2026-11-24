package com.bookstore.sales.domain;

import com.bookstore.shared.kernel.validation.Guard;

import java.time.Instant;
import java.util.Locale;

/**
 * The administrator's order-list filters after dates have been converted to UTC boundaries.
 *
 * @param status        the status, or {@code null} for every status
 * @param fromInclusive the first instant, or {@code null}
 * @param untilExclusive the exclusive upper instant, or {@code null}
 * @param orderNumber   a case-insensitive fragment, or {@code null}
 */
public record OrderSearchCriteria(OrderStatus status, Instant fromInclusive, Instant untilExclusive,
                                  String orderNumber) {

    /** Rejects an inverted range and normalizes the optional order-number fragment. */
    public OrderSearchCriteria {
        if (fromInclusive != null && untilExclusive != null) {
            Guard.require(fromInclusive.isBefore(untilExclusive), "from", "must be before the exclusive end");
        }
        orderNumber = Guard.optionalText(orderNumber, "orderNumber", 14);
        if (orderNumber != null) {
            orderNumber = orderNumber.toUpperCase(Locale.ROOT);
        }
    }
}
