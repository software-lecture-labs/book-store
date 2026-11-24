package com.bookstore.sales.domain;

import com.bookstore.shared.kernel.error.ConflictException;

/**
 * Signals that a cart or order asks for more copies than are available. Maps to HTTP 409.
 *
 * <p>It carries the same code as the catalog's own stock error ({@code stock.insufficient}), so
 * clients see one error whichever module noticed the shortage.
 */
public class StockShortageException extends ConflictException {

    /** Error code for insufficient stock. */
    public static final String CODE = "stock.insufficient";

    private final String bookTitle;
    private final int requested;
    private final int available;

    /**
     * Creates the exception.
     *
     * @param bookTitle title of the book, for the message shown to the customer
     * @param requested copies requested
     * @param available copies available
     */
    public StockShortageException(String bookTitle, int requested, int available) {
        super(CODE, "Only " + available + " of '" + bookTitle + "' available, " + requested + " requested");
        this.bookTitle = bookTitle;
        this.requested = requested;
        this.available = available;
    }

    /** @return the title of the book */
    public String bookTitle() {
        return bookTitle;
    }

    /** @return how many copies were requested */
    public int requested() {
        return requested;
    }

    /** @return how many copies were available */
    public int available() {
        return available;
    }
}
