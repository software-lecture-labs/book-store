package com.bookstore.catalog.api;

import com.bookstore.shared.kernel.money.Money;
import com.bookstore.shared.kernel.paging.SortOrder;
import com.bookstore.shared.kernel.validation.Guard;

/**
 * A customer's catalog search as it comes from the search form. It names one category; the
 * catalog widens it to the category and its subcategories (CAT-5).
 *
 * <p>A part the customer left empty is {@code null}. Codes are checked when the search runs: an
 * unknown language or sort field is reported as a validation error.
 *
 * @param text        words to find in the title, an author's name or the ISBN; may be {@code null}
 * @param categoryId  the chosen category; may be {@code null}
 * @param authorId    the chosen author; may be {@code null}
 * @param minPrice    lowest price; may be {@code null}
 * @param maxPrice    highest price; may be {@code null}
 * @param language    ISO 639-1 code of the required language, for example {@code "en"}; may be {@code null}
 * @param inStockOnly whether to leave out books with no stock
 * @param sortBy      what to sort by: {@code TITLE}, {@code PRICE}, {@code PUBLICATION_YEAR} or {@code RATING}
 * @param order       the direction of the sort
 */
public record BookSearchQuery(String text, Long categoryId, Long authorId, Money minPrice, Money maxPrice,
                              String language, boolean inStockOnly, String sortBy, SortOrder order) {

    /** The sort field of a query that does not choose one. */
    public static final String DEFAULT_SORT = "TITLE";

    /** Rejects a missing order and an inverted price range. */
    public BookSearchQuery {
        Guard.requireNonNull(sortBy, "sortBy");
        Guard.requireNonNull(order, "order");
        if (minPrice != null && maxPrice != null) {
            Guard.require(maxPrice.isAtLeast(minPrice), "maxPrice", "must not be below the minimum price");
        }
    }

    /**
     * Returns a query that lists every book by title.
     *
     * @return the query
     */
    public static BookSearchQuery everything() {
        return new BookSearchQuery(null, null, null, null, null, null, false, DEFAULT_SORT, SortOrder.ASC);
    }

    /**
     * Returns a copy that searches for some text.
     *
     * @param value the text, may be {@code null}
     * @return the new query
     */
    public BookSearchQuery withText(String value) {
        return new BookSearchQuery(value, categoryId, authorId, minPrice, maxPrice, language, inStockOnly, sortBy,
                order);
    }

    /**
     * Returns a copy limited to a category and its subcategories.
     *
     * @param value the category's id
     * @return the new query
     */
    public BookSearchQuery inCategory(long value) {
        return new BookSearchQuery(text, value, authorId, minPrice, maxPrice, language, inStockOnly, sortBy, order);
    }

    /**
     * Returns a copy limited to one author's books.
     *
     * @param value the author's id
     * @return the new query
     */
    public BookSearchQuery byAuthor(long value) {
        return new BookSearchQuery(text, categoryId, value, minPrice, maxPrice, language, inStockOnly, sortBy, order);
    }

    /**
     * Returns a copy limited to a price range.
     *
     * @param min lowest price, may be {@code null}
     * @param max highest price, may be {@code null}
     * @return the new query
     */
    public BookSearchQuery priced(Money min, Money max) {
        return new BookSearchQuery(text, categoryId, authorId, min, max, language, inStockOnly, sortBy, order);
    }

    /**
     * Returns a copy limited to one language.
     *
     * @param code the language's ISO 639-1 code
     * @return the new query
     */
    public BookSearchQuery inLanguage(String code) {
        return new BookSearchQuery(text, categoryId, authorId, minPrice, maxPrice, code, inStockOnly, sortBy, order);
    }

    /**
     * Returns a copy limited to books in stock.
     *
     * @return the new query
     */
    public BookSearchQuery onlyInStock() {
        return new BookSearchQuery(text, categoryId, authorId, minPrice, maxPrice, language, true, sortBy, order);
    }

    /**
     * Returns a copy with another order.
     *
     * @param field     what to sort by, for example {@code "PRICE"}
     * @param direction the direction
     * @return the new query
     */
    public BookSearchQuery sortedBy(String field, SortOrder direction) {
        return new BookSearchQuery(text, categoryId, authorId, minPrice, maxPrice, language, inStockOnly, field,
                direction);
    }
}
