package com.bookstore.catalog.domain;

import com.bookstore.shared.kernel.error.ConflictException;

/**
 * Signals that fewer copies are available than were requested. Maps to HTTP 409.
 */
public class InsufficientStockException extends ConflictException {

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
    public InsufficientStockException(String bookTitle, int requested, int available) {
        super(CODE, "Only " + available + " of '" + bookTitle + "' available, " + requested + " requested");
        this.bookTitle = bookTitle;
        this.requested = requested;
        this.available = available;
    }

    /**
     * Returns the title of the book.
     *
     * @return the title
     */
    public String bookTitle() {
        return bookTitle;
    }

    /**
     * Returns how many copies were requested.
     *
     * @return the requested quantity
     */
    public int requested() {
        return requested;
    }

    /**
     * Returns how many copies were available.
     *
     * @return the available quantity
     */
    public int available() {
        return available;
    }
}
