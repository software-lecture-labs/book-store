package com.bookstore.sales.domain;

import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.money.Money;

/**
 * A change {@link Cart#reconcile} made to a cart because the catalog changed since the customer
 * last looked. Shown to the customer as a warning.
 */
public sealed interface CartAdjustment {

    /**
     * Returns the affected book's id.
     *
     * @return the id
     */
    BookId bookId();

    /**
     * Returns the affected book's title, for the message.
     *
     * @return the title
     */
    String bookTitle();

    /**
     * The book is no longer sold or is out of stock, so its line was removed.
     *
     * @param bookId    the book's id
     * @param bookTitle the title, or {@code "#id"} if the book no longer exists
     */
    record Removed(BookId bookId, String bookTitle) implements CartAdjustment {
    }

    /**
     * The stock dropped below the quantity in the cart, so the quantity was lowered.
     *
     * @param bookId    the book's id
     * @param bookTitle the title
     * @param from      the quantity before
     * @param to        the quantity now, equal to the stock
     */
    record QuantityReduced(BookId bookId, String bookTitle, int from, int to) implements CartAdjustment {
    }

    /**
     * The price changed since the book was added.
     *
     * @param bookId    the book's id
     * @param bookTitle the title
     * @param from      the price the customer saw
     * @param to        the current price
     */
    record PriceChanged(BookId bookId, String bookTitle, Money from, Money to) implements CartAdjustment {
    }
}
