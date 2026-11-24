package com.bookstore.catalog.api;

import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.money.Money;

import java.math.BigDecimal;

/**
 * What a book list shows for each book. A read-only copy, so the web layer cannot change the
 * book by accident.
 *
 * @param id            the book's id
 * @param isbn          the ISBN-13
 * @param title         the title
 * @param authorNames   the authors, joined with commas
 * @param price         the current price
 * @param inStock       whether at least one copy can be ordered
 * @param averageRating the average rating with one decimal place; {@code null} if nobody rated the book
 * @param ratingCount   the number of ratings
 * @param coverImage    the storage key of the cover image; {@code null} if there is none
 */
public record BookSummary(BookId id, String isbn, String title, String authorNames, Money price, boolean inStock,
                          BigDecimal averageRating, int ratingCount, String coverImage) {
}
