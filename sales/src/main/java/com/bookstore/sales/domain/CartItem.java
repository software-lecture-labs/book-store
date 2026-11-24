package com.bookstore.sales.domain;

import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.money.Money;
import com.bookstore.shared.kernel.validation.Guard;

import java.io.Serializable;

/**
 * One line of a cart: a book and how many copies of it.
 *
 * <p>The cart always charges the book's <em>current</em> price. The price seen when the line was
 * added is kept only so that {@link Cart#reconcile} can tell the customer that the price changed.
 *
 * @param bookId         the book's id
 * @param quantity       copies, from 1 to {@link Cart#MAX_QUANTITY_PER_LINE}
 * @param priceWhenAdded the unit price the customer last saw
 */
public record CartItem(BookId bookId, int quantity, Money priceWhenAdded) implements Serializable {

    /** Validates every part. */
    public CartItem {
        Guard.requireNonNull(bookId, "bookId");
        Guard.requireBetween(quantity, 1, Cart.MAX_QUANTITY_PER_LINE, "quantity");
        Guard.requireNonNull(priceWhenAdded, "priceWhenAdded");
        Guard.require(priceWhenAdded.isPositive(), "priceWhenAdded", "must be greater than zero");
    }

    CartItem withQuantity(int newQuantity) {
        return new CartItem(bookId, newQuantity, priceWhenAdded);
    }

    CartItem withPrice(Money newPrice) {
        return new CartItem(bookId, quantity, newPrice);
    }
}
