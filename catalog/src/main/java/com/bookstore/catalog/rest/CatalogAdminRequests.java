package com.bookstore.catalog.rest;

import com.bookstore.catalog.api.BookCommand;
import com.bookstore.shared.kernel.money.Money;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * The request bodies of the catalog's administration endpoints. Bean Validation checks only their
 * format; the domain checks the rules (ADR-0009).
 */
public final class CatalogAdminRequests {

    private CatalogAdminRequests() {
    }

    /**
     * A book as the administrator enters it (ADM-1).
     *
     * @param isbn            the ISBN; read only when the book is added
     * @param title           the title
     * @param authorIds       the authors, in order
     * @param categoryId      the category
     * @param publisher       the publisher, or {@code null}
     * @param publicationYear the year, or {@code null}
     * @param language        the language code, for example {@code tr}
     * @param pageCount       the number of pages, zero if unknown
     * @param description     the description, or {@code null}
     * @param price           the price in Turkish lira
     * @param stock           copies in stock
     */
    public record BookRequest(
            @Size(max = 20) String isbn,
            @NotBlank @Size(max = 300) String title,
            @NotEmpty List<@NotNull @Positive Long> authorIds,
            @NotNull @Positive Long categoryId,
            @Size(max = 200) String publisher,
            Integer publicationYear,
            @NotBlank @Size(max = 2) String language,
            @PositiveOrZero int pageCount,
            @Size(max = 5000) String description,
            @NotNull @DecimalMin("0.01") BigDecimal price,
            @PositiveOrZero int stock) {

        BookCommand toCommand() {
            return new BookCommand(isbn, title, authorIds, categoryId, publisher, publicationYear, language, pageCount,
                    description, Money.of(price), stock);
        }
    }

    /**
     * A new price (ADM-3).
     *
     * @param price the price in Turkish lira
     */
    public record PriceRequest(@NotNull @DecimalMin("0.01") BigDecimal price) {
    }

    /**
     * A new stock count (ADM-3).
     *
     * @param stock copies in stock
     */
    public record StockRequest(@NotNull @PositiveOrZero Integer stock) {
    }

    /**
     * Whether something is in use: a book on sale, an account that may sign in.
     *
     * @param active the new state
     */
    public record ActiveRequest(@NotNull Boolean active) {
    }

    /**
     * A name for an author or a category (ADM-2).
     *
     * @param name the name
     */
    public record NameRequest(@NotBlank @Size(max = 200) String name) {
    }

    /**
     * A new category (ADM-2).
     *
     * @param name     the name
     * @param parentId the parent category, or {@code null} for a top-level one
     */
    public record CategoryRequest(@NotBlank @Size(max = 100) String name, @Positive Long parentId) {
    }
}
