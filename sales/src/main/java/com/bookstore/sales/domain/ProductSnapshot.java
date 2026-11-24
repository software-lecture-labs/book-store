package com.bookstore.sales.domain;

import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.money.Money;
import com.bookstore.shared.kernel.validation.Guard;

/**
 * What the sales module knows about a book at one moment: its title, price, stock and whether it is
 * still sold. Carts and orders work with this copy instead of the catalog's {@code Book} aggregate,
 * so the sales domain does not depend on the catalog module (ADR-0002, ADR-0003). The application
 * layer fills it from {@code catalog.api.BookOffer}.
 *
 * @param bookId         the book's id
 * @param title          the title
 * @param authorNames    the authors, joined with commas
 * @param unitPrice      the current price of one copy
 * @param availableStock copies that can be ordered now
 * @param active         whether the book is still sold
 */
public record ProductSnapshot(BookId bookId, String title, String authorNames, Money unitPrice, int availableStock,
                              boolean active) {

    /** Longest title the catalog allows; order lines copy titles up to this length. */
    public static final int MAX_TITLE_LENGTH = 300;

    /** Validates the parts. */
    public ProductSnapshot {
        Guard.requireNonNull(bookId, "bookId");
        title = Guard.requireText(title, "title", MAX_TITLE_LENGTH);
        authorNames = Guard.optionalText(authorNames, "authorNames", 1000);
        Guard.requireNonNull(unitPrice, "unitPrice");
        Guard.require(unitPrice.isPositive(), "unitPrice", "must be greater than zero");
        Guard.requireNonNegative(availableStock, "availableStock");
    }

    /**
     * Tells whether at least one copy can be ordered.
     *
     * @return {@code true} if the book is sold and in stock
     */
    public boolean isAvailableForSale() {
        return active && availableStock > 0;
    }

    /**
     * Tells whether the given number of copies can be ordered.
     *
     * @param quantity copies wanted
     * @return {@code true} if there are enough
     */
    public boolean hasAtLeast(int quantity) {
        return availableStock >= quantity;
    }
}
