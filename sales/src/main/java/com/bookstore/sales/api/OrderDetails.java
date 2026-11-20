package com.bookstore.sales.api;

import com.bookstore.shared.kernel.address.Address;
import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.kernel.money.Money;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * An order's detail page.
 *
 * @param orderNumber     the order number
 * @param customerId      the customer who placed it
 * @param placedAt        when it was placed
 * @param statusChangedAt when the status last changed
 * @param status          the current status, for example {@code SHIPPED}
 * @param nextStatuses    the statuses an administrator may move it to
 * @param lines           the lines, with the titles and prices at the time of the order
 * @param shippingAddress the delivery address
 * @param paymentMethod   how it is paid: {@code CREDIT_CARD} or {@code BANK_TRANSFER}
 * @param subtotal        the price of the books
 * @param shippingCost    the shipping cost
 * @param total           the amount to pay
 */
public record OrderDetails(String orderNumber, CustomerId customerId, Instant placedAt, Instant statusChangedAt,
                           String status, Set<String> nextStatuses, List<Line> lines,
                           Address shippingAddress, String paymentMethod, Money subtotal, Money shippingCost,
                           Money total) {

    /** Copies the collections. */
    public OrderDetails {
        nextStatuses = Set.copyOf(nextStatuses);
        lines = List.copyOf(lines);
    }

    /**
     * One line of an order, with the title and price at the time of the order.
     *
     * @param bookId    the book
     * @param bookTitle the title
     * @param unitPrice the price of one copy
     * @param quantity  the number of copies
     * @param lineTotal the price of the line
     */
    public record Line(BookId bookId, String bookTitle, Money unitPrice, int quantity, Money lineTotal) {
    }
}
