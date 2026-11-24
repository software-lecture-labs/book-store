package com.bookstore.catalog.domain;

import com.bookstore.shared.kernel.paging.SortOrder;
import com.bookstore.shared.kernel.text.TextNormalizer;
import com.bookstore.shared.kernel.validation.Guard;

import java.util.Comparator;

/**
 * How to order a list of books: an attribute and a direction.
 *
 * @param field the attribute to sort by
 * @param order the direction
 */
public record BookSort(BookSortField field, SortOrder order) {

    /** The order used when the customer does not choose one: by title, A to Z. */
    public static final BookSort DEFAULT = new BookSort(BookSortField.TITLE, SortOrder.ASC);

    /** Rejects missing parts. */
    public BookSort {
        Guard.requireNonNull(field, "sort");
        Guard.requireNonNull(order, "order");
    }

    /**
     * Returns a comparator for this sort. Books that are equal on the chosen attribute are ordered
     * by title and then by ISBN, so the order is always the same and paging is stable.
     *
     * @return the comparator
     */
    public Comparator<Book> comparator() {
        Comparator<Book> byTitle = Comparator.comparing(book -> TextNormalizer.searchKey(book.title()));
        return order.apply(field.ascending())
                .thenComparing(byTitle)
                .thenComparing(book -> book.isbn().value());
    }
}
