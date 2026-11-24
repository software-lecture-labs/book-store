package com.bookstore.catalog.application;

import com.bookstore.catalog.api.AdminBookRow;
import com.bookstore.catalog.api.AuthorSummary;
import com.bookstore.catalog.api.AuthorView;
import com.bookstore.catalog.api.BookDetails;
import com.bookstore.catalog.api.BookSearchQuery;
import com.bookstore.catalog.api.BookSummary;
import com.bookstore.catalog.api.CategoryNode;
import com.bookstore.catalog.domain.Author;
import com.bookstore.catalog.domain.Book;
import com.bookstore.catalog.domain.BookSort;
import com.bookstore.catalog.domain.BookSortField;
import com.bookstore.catalog.domain.Category;
import com.bookstore.catalog.domain.Language;
import com.bookstore.shared.kernel.error.ValidationException;

import java.util.Locale;

/**
 * Translates between the catalog's domain and its {@code api} contract (Mapper, ADR-0003). Domain
 * objects never leave the module; the contract carries ids as numbers and enums as codes.
 */
final class CatalogMapper {

    private CatalogMapper() {
    }

    static BookSummary summaryOf(Book book) {
        return new BookSummary(book.id(), book.isbn().value(), book.title(), book.authorNames(), book.price(),
                book.isAvailableForSale(), book.ratings().average().orElse(null), book.ratings().count(),
                book.coverImage().orElse(null));
    }

    static BookDetails detailsOf(Book book) {
        return new BookDetails(book.id(), book.isbn().value(), book.title(),
                book.authors().stream().map(CatalogMapper::authorOf).toList(), book.category().id().value(),
                book.category().path(), book.publisher(), book.publicationYear().orElse(null),
                book.language().code(), book.pageCount(), book.description(), book.price(), book.stock().quantity(),
                book.isAvailableForSale(), book.ratings().average().orElse(null), book.ratings().count(),
                book.coverImage().orElse(null), book.isActive(), book.version());
    }

    static AdminBookRow rowOf(Book book) {
        return new AdminBookRow(book.id(), book.isbn().value(), book.title(), book.authorNames(), book.category().name(),
                book.price(), book.stock().quantity(), book.isActive(), book.version());
    }

    static AuthorView authorViewOf(Author author) {
        return new AuthorView(author.id().value(), author.name());
    }

    static AuthorSummary authorSummaryOf(Author author) {
        return new AuthorSummary(author.id().value(), author.name());
    }

    static CategoryNode nodeOf(Category category) {
        return new CategoryNode(category.id().value(), category.name(),
                category.children().stream().map(CatalogMapper::nodeOf).toList());
    }

    /**
     * Reads the sort of a query.
     *
     * @throws ValidationException if the field is unknown
     */
    static BookSort sortOf(BookSearchQuery query) {
        try {
            return new BookSort(BookSortField.valueOf(query.sortBy().toUpperCase(Locale.ROOT)), query.order());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("sortBy", "unknown sort field: " + query.sortBy());
        }
    }

    /**
     * Reads a language code.
     *
     * @throws ValidationException if the code is unknown
     */
    static Language languageOf(String code) {
        return Language.fromCode(code);
    }

    private static BookDetails.AuthorRef authorOf(Author author) {
        return new BookDetails.AuthorRef(author.id().value(), author.name());
    }
}
