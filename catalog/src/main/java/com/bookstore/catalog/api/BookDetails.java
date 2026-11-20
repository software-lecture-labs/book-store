package com.bookstore.catalog.api;

import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.money.Money;

import java.math.BigDecimal;
import java.time.Year;
import java.util.List;

/**
 * Everything a book's detail page shows.
 *
 * @param id              the book's id
 * @param isbn            the ISBN-13
 * @param title           the title
 * @param authors         the authors, in order
 * @param categoryId      the category's id
 * @param categoryPath    the category names from the root down, for breadcrumbs
 * @param publisher       the publisher, or an empty string
 * @param publicationYear the year of publication; {@code null} if unknown
 * @param language        the language's ISO 639-1 code, for example {@code "tr"}
 * @param pageCount       the number of pages, zero if unknown
 * @param description     the description, or an empty string
 * @param price           the current price
 * @param stock           copies available
 * @param inStock         whether at least one copy can be ordered
 * @param averageRating   the average rating; {@code null} if nobody rated the book
 * @param ratingCount     the number of ratings
 * @param coverImage      the storage key of the cover image; {@code null} if there is none
 * @param active          whether the book is sold; customers only ever see books on sale
 * @param version         the stored version; the administrator's API answers it as the entity tag (ADR-0022)
 */
public record BookDetails(BookId id, String isbn, String title, List<AuthorRef> authors, long categoryId,
                          List<String> categoryPath, String publisher, Year publicationYear,
                          String language, int pageCount, String description, Money price, int stock,
                          boolean inStock, BigDecimal averageRating, int ratingCount, String coverImage,
                          boolean active, int version) {

    /** Copies the lists. */
    public BookDetails {
        authors = List.copyOf(authors);
        categoryPath = List.copyOf(categoryPath);
    }

    /**
     * An author, with the id needed to link to the author's other books.
     *
     * @param id   the author's id
     * @param name the author's name
     */
    public record AuthorRef(long id, String name) {
    }
}
