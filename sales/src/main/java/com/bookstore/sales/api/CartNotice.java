package com.bookstore.sales.api;

import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.money.Money;

/**
 * Something the cart changed on its own since the customer last looked, because the catalog
 * changed (CRT-5): a book is no longer sold, fewer copies are left, or the price moved.
 */
public sealed interface CartNotice {

    /** @return the book the notice is about */
    BookId bookId();

    /** @return the book's title, or {@code "#<id>"} if the catalog no longer knows it */
    String bookTitle();

    /**
     * The book was taken out of the cart.
     *
     * @param bookId    the book
     * @param bookTitle the title
     */
    record Removed(BookId bookId, String bookTitle) implements CartNotice {
    }

    /**
     * Fewer copies are left than the cart asked for.
     *
     * @param bookId    the book
     * @param bookTitle the title
     * @param from      the quantity before
     * @param to        the quantity now
     */
    record QuantityReduced(BookId bookId, String bookTitle, int from, int to) implements CartNotice {
    }

    /**
     * The price changed since the book was added.
     *
     * @param bookId    the book
     * @param bookTitle the title
     * @param from      the price when it was added
     * @param to        the current price
     */
    record PriceChanged(BookId bookId, String bookTitle, Money from, Money to) implements CartNotice {
    }
}
