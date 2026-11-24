package com.bookstore.catalog.api;

import com.bookstore.shared.kernel.money.Money;

import java.util.List;

/**
 * A book as the administrator enters it (ADM-1). The domain checks every value.
 *
 * @param isbn            the ISBN-10 or ISBN-13, with or without hyphens; only read when the book is added
 * @param title           the title
 * @param authorIds       the authors, in order, at least one
 * @param categoryId      the category
 * @param publisher       the publisher; {@code null} if unknown
 * @param publicationYear the year of publication; {@code null} if unknown
 * @param language        the language's ISO 639-1 code, for example {@code "tr"}
 * @param pageCount       the number of pages, zero if unknown
 * @param description     the description; {@code null} for none
 * @param price           the price
 * @param stock           copies in stock
 */
public record BookCommand(String isbn, String title, List<Long> authorIds, long categoryId, String publisher,
                          Integer publicationYear, String language, int pageCount, String description, Money price,
                          int stock) {

    /** Copies the authors; a missing list counts as empty. */
    public BookCommand {
        authorIds = authorIds == null ? List.of() : List.copyOf(authorIds);
    }
}
