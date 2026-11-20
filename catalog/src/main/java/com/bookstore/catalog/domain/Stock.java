package com.bookstore.catalog.domain;

import com.bookstore.shared.kernel.validation.Guard;

/**
 * The number of copies of a book that can be sold.
 *
 * @param quantity copies on hand, never negative
 */
public record Stock(int quantity) {

    /** No copies. */
    public static final Stock EMPTY = new Stock(0);

    /** Rejects negative quantities. */
    public Stock {
        Guard.requireNonNegative(quantity, "stock");
    }

    /**
     * Returns a stock of the given size.
     *
     * @param quantity copies on hand
     * @return the stock
     */
    public static Stock of(int quantity) {
        return new Stock(quantity);
    }

    /**
     * Tells whether at least one copy is available.
     *
     * @return {@code true} if the quantity is positive
     */
    public boolean isAvailable() {
        return quantity > 0;
    }

    /**
     * Tells whether the requested number of copies is available.
     *
     * @param requested copies wanted
     * @return {@code true} if there are enough copies
     */
    public boolean hasAtLeast(int requested) {
        return quantity >= requested;
    }

    /**
     * Returns the stock after taking copies away.
     *
     * @param copies copies to take, positive and not more than available
     * @return the reduced stock
     */
    public Stock minus(int copies) {
        Guard.requirePositive(copies, "quantity");
        return new Stock(quantity - copies);
    }

    /**
     * Returns the stock after adding copies.
     *
     * @param copies copies to add, positive
     * @return the increased stock
     */
    public Stock plus(int copies) {
        Guard.requirePositive(copies, "quantity");
        return new Stock(Math.addExact(quantity, copies));
    }
}
