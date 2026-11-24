package com.bookstore.catalog.api;

import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.money.Money;

/**
 * What another module needs to know about a book in order to sell it: its current title, price,
 * stock and whether it is still sold. A copy taken at the moment of the call.
 *
 * @param bookId         the book's id
 * @param title          the current title
 * @param authorNames    the authors, joined with commas
 * @param price          the current price
 * @param availableStock copies that can be ordered now
 * @param active         whether the book is still sold
 */
public record BookOffer(BookId bookId, String title, String authorNames, Money price, int availableStock,
                        boolean active) {
}
