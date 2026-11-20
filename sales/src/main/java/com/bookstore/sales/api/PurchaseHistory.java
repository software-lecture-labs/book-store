package com.bookstore.sales.api;

import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.identity.CustomerId;

/**
 * What other modules may ask about customers' purchases (ADR-0003). The review module uses it to
 * allow reviews only from customers who received the book (REV-2).
 */
public interface PurchaseHistory {

    /**
     * Tells whether a customer has received a book.
     *
     * @param customerId the customer's id
     * @param bookId     the book's id
     * @return {@code true} if a delivered order of the customer contains the book
     */
    boolean hasReceived(CustomerId customerId, BookId bookId);
}
