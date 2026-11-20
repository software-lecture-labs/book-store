package com.bookstore.sales.domain;

import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.money.Money;
import com.bookstore.shared.kernel.validation.Guard;

/**
 * One line of an order. The book's title and price are copied when the order is placed, so the
 * order keeps showing what the customer actually bought and paid, whatever happens to the book
 * later.
 *
 * @param bookId    the book's id
 * @param bookTitle the title at the time of the order
 * @param unitPrice the price of one copy at the time of the order
 * @param quantity  copies ordered
 */
public record OrderLine(BookId bookId, String bookTitle, Money unitPrice, int quantity) {

    /** Validates every part. */
    public OrderLine {
        Guard.requireNonNull(bookId, "bookId");
        bookTitle = Guard.requireText(bookTitle, "bookTitle", ProductSnapshot.MAX_TITLE_LENGTH);
        Guard.requireNonNull(unitPrice, "unitPrice");
        Guard.require(unitPrice.isPositive(), "unitPrice", "must be greater than zero");
        Guard.requirePositive(quantity, "quantity");
    }

    /**
     * Copies the current title and price of a product.
     *
     * @param product  the book as currently offered
     * @param quantity copies ordered
     * @return the line
     */
    public static OrderLine of(ProductSnapshot product, int quantity) {
        return new OrderLine(product.bookId(), product.title(), product.unitPrice(), quantity);
    }

    /**
     * Returns the price of the whole line.
     *
     * @return unit price times quantity
     */
    public Money lineTotal() {
        return unitPrice.times(quantity);
    }
}
