package com.bookstore.catalog.api;

import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.money.Money;

/**
 * A book in the administrator's list, deactivated ones included (ADM-1, ADM-3).
 *
 * @param id          the book's id
 * @param isbn        the ISBN-13
 * @param title       the title
 * @param authorNames the authors, joined with commas
 * @param category    the category's name
 * @param price       the current price
 * @param stock       copies in stock
 * @param active      whether the book is sold
 * @param version     the stored version, for {@code If-Match} (ADR-0022)
 */
public record AdminBookRow(BookId id, String isbn, String title, String authorNames, String category, Money price,
                           int stock, boolean active, int version) {
}
