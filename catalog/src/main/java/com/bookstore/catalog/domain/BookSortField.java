package com.bookstore.catalog.domain;

import com.bookstore.shared.kernel.text.TextNormalizer;

import java.time.Year;
import java.util.Comparator;

/**
 * The attributes a book list can be sorted by. Each constant knows how to compare two books in
 * ascending order (Strategy pattern).
 */
public enum BookSortField {

    /** Alphabetically by title, ignoring case and Turkish letters. */
    TITLE(Comparator.comparing(book -> TextNormalizer.searchKey(book.title()))),

    /** By current price. */
    PRICE(Comparator.comparing(Book::price)),

    /** By publication year; books without a year come first. */
    PUBLICATION_YEAR(Comparator.comparing(
            (Book book) -> book.publicationYear().orElse(null),
            Comparator.nullsFirst(Comparator.<Year>naturalOrder()))),

    /** By average rating; unrated books count as zero. */
    RATING(Comparator.comparingDouble(book -> book.ratings().sortKey()));

    private final Comparator<Book> ascending;

    BookSortField(Comparator<Book> ascending) {
        this.ascending = ascending;
    }

    /**
     * Returns a comparator that sorts books by this attribute, smallest first.
     *
     * @return the ascending comparator
     */
    public Comparator<Book> ascending() {
        return ascending;
    }
}
