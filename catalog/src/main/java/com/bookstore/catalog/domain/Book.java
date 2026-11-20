package com.bookstore.catalog.domain;

import com.bookstore.shared.kernel.domain.Entity;
import com.bookstore.shared.kernel.error.ConflictException;
import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.money.Money;
import com.bookstore.shared.kernel.validation.Guard;

import java.time.Year;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * A book offered for sale.
 *
 * <p>Books are created with a {@link Builder} because they have many attributes, most of them
 * required:
 *
 * <pre>{@code
 * Book book = Book.builder()
 *         .isbn(Isbn.parse("978-975-07-1938-7"))
 *         .title("Kürk Mantolu Madonna")
 *         .author(sabahattinAli)
 *         .category(novels)
 *         .price(Money.of("120.00"))
 *         .build();
 * }</pre>
 *
 * <p>A book is never deleted, because past orders refer to it. Instead it is
 * {@link #deactivate() deactivated}, which hides it from customers.
 */
public class Book extends Entity<BookId> {

    /** Longest allowed title. */
    public static final int MAX_TITLE_LENGTH = 300;

    /** Longest allowed publisher name. */
    public static final int MAX_PUBLISHER_LENGTH = 200;

    /** Longest allowed description. */
    public static final int MAX_DESCRIPTION_LENGTH = 5000;

    /** Earliest accepted publication year (the printing press). */
    public static final Year EARLIEST_YEAR = Year.of(1450);

    private final Isbn isbn;
    private String title;
    private final Set<Author> authors = new LinkedHashSet<>();
    private Category category;
    private String publisher;
    private Year publicationYear;
    private Language language;
    private int pageCount;
    private String description;
    private Money price;
    private Stock stock;
    private String coverImage;
    private boolean active;
    private RatingSummary ratings;

    private Book(Builder builder) {
        super();
        this.isbn = Guard.requireNonNull(builder.isbn, "isbn");
        this.title = validTitle(builder.title);
        replaceAuthors(builder.authors);
        this.category = Guard.requireNonNull(builder.category, "category");
        this.publisher = Guard.optionalText(builder.publisher, "publisher", MAX_PUBLISHER_LENGTH);
        this.publicationYear = validYear(builder.publicationYear);
        this.language = Guard.requireNonNull(builder.language, "language");
        this.pageCount = Guard.requireNonNegative(builder.pageCount, "pageCount");
        this.description = Guard.optionalText(builder.description, "description", MAX_DESCRIPTION_LENGTH);
        this.price = validPrice(builder.price);
        this.stock = Guard.requireNonNull(builder.stock, "stock");
        this.coverImage = builder.coverImage;
        this.active = builder.active;
        this.ratings = Guard.requireNonNull(builder.ratings, "ratings");
        if (builder.id != null) {
            markPersisted(builder.id, builder.version);
        }
    }

    /**
     * Starts building a book.
     *
     * @return an empty builder
     */
    public static Builder builder() {
        return new Builder();
    }

    // ---------------------------------------------------------------- queries

    /** @return the ISBN, which never changes */
    public Isbn isbn() {
        return isbn;
    }

    /** @return the title */
    public String title() {
        return title;
    }

    /** @return the authors in the order they were given, never empty */
    public Set<Author> authors() {
        return Collections.unmodifiableSet(authors);
    }

    /** @return the category */
    public Category category() {
        return category;
    }

    /** @return the publisher, or an empty string */
    public String publisher() {
        return publisher;
    }

    /** @return the year of publication, if known */
    public Optional<Year> publicationYear() {
        return Optional.ofNullable(publicationYear);
    }

    /** @return the language */
    public Language language() {
        return language;
    }

    /** @return the number of pages, zero if unknown */
    public int pageCount() {
        return pageCount;
    }

    /** @return the description, or an empty string */
    public String description() {
        return description;
    }

    /** @return the current price */
    public Money price() {
        return price;
    }

    /** @return the current stock */
    public Stock stock() {
        return stock;
    }

    /** @return the storage key of the cover image, if one was uploaded */
    public Optional<String> coverImage() {
        return Optional.ofNullable(coverImage);
    }

    /** @return {@code true} if customers can see the book */
    public boolean isActive() {
        return active;
    }

    /** @return the rating totals */
    public RatingSummary ratings() {
        return ratings;
    }

    /**
     * Tells whether customers can put the book in their cart: it is active and in stock.
     *
     * @return {@code true} if the book can be sold
     */
    public boolean isAvailableForSale() {
        return active && stock.isAvailable();
    }

    /**
     * Returns the authors' names joined with commas, for display and search.
     *
     * @return for example {@code "Neil Gaiman, Terry Pratchett"}
     */
    public String authorNames() {
        return String.join(", ", authors.stream().map(Author::name).toList());
    }

    // ---------------------------------------------------------------- commands

    /**
     * Changes the descriptive attributes.
     *
     * @param newTitle       the title
     * @param newPublisher   the publisher, may be {@code null} or blank
     * @param newYear        the publication year, may be {@code null}
     * @param newLanguage    the language
     * @param newPageCount   the number of pages, zero if unknown
     * @param newDescription the description, may be {@code null} or blank
     */
    public void changeDetails(String newTitle, String newPublisher, Year newYear, Language newLanguage,
                              int newPageCount, String newDescription) {
        String validTitle = validTitle(newTitle);
        String validPublisher = Guard.optionalText(newPublisher, "publisher", MAX_PUBLISHER_LENGTH);
        Year validYear = validYear(newYear);
        Language validLanguage = Guard.requireNonNull(newLanguage, "language");
        int validPageCount = Guard.requireNonNegative(newPageCount, "pageCount");
        String validDescription = Guard.optionalText(newDescription, "description", MAX_DESCRIPTION_LENGTH);

        this.title = validTitle;
        this.publisher = validPublisher;
        this.publicationYear = validYear;
        this.language = validLanguage;
        this.pageCount = validPageCount;
        this.description = validDescription;
    }

    /**
     * Replaces the authors.
     *
     * @param newAuthors at least one author
     */
    public void changeAuthors(Collection<Author> newAuthors) {
        replaceAuthors(newAuthors);
    }

    /**
     * Moves the book to another category.
     *
     * @param newCategory the category
     */
    public void moveTo(Category newCategory) {
        this.category = Guard.requireNonNull(newCategory, "category");
    }

    /**
     * Changes the price. Orders already placed keep the price they were placed with.
     *
     * @param newPrice a positive amount
     */
    public void changePrice(Money newPrice) {
        this.price = validPrice(newPrice);
    }

    /**
     * Sets the stock to an exact quantity, for example after counting the warehouse.
     *
     * @param quantity copies on hand
     */
    public void changeStock(int quantity) {
        this.stock = Stock.of(quantity);
    }

    /**
     * Takes copies out of stock for an order.
     *
     * @param quantity copies to take, positive
     * @throws InsufficientStockException if fewer copies are available
     * @throws ConflictException          if the book is no longer sold
     */
    public void reserve(int quantity) {
        Guard.requirePositive(quantity, "quantity");
        if (!active) {
            throw new ConflictException("book.inactive", "'" + title + "' is no longer sold");
        }
        if (!stock.hasAtLeast(quantity)) {
            throw new InsufficientStockException(title, quantity, stock.quantity());
        }
        this.stock = stock.minus(quantity);
    }

    /**
     * Puts copies back into stock, for example when an order is cancelled.
     *
     * @param quantity copies to return, positive
     */
    public void release(int quantity) {
        this.stock = stock.plus(quantity);
    }

    /**
     * Records the storage key of an uploaded cover image.
     *
     * @param storageKey the key returned by the file storage
     */
    public void attachCover(String storageKey) {
        this.coverImage = Guard.requireText(storageKey, "coverImage", 255);
    }

    /** Hides the book from customers. Past orders keep showing it. */
    public void deactivate() {
        this.active = false;
    }

    /** Shows the book to customers again. */
    public void activate() {
        this.active = true;
    }

    /**
     * Adds a customer's rating to the totals.
     *
     * @param stars star value from 1 to 5
     */
    public void addRating(int stars) {
        this.ratings = ratings.with(stars);
    }

    /**
     * Removes a customer's rating from the totals, for example when a review is deleted.
     *
     * @param stars the star value that was added before
     */
    public void removeRating(int stars) {
        this.ratings = ratings.without(stars);
    }

    @Override
    public String toString() {
        return "Book[isbn=" + isbn + ", title=" + title + "]";
    }

    // ---------------------------------------------------------------- validation

    private void replaceAuthors(Collection<Author> newAuthors) {
        Guard.requireNotEmpty(newAuthors, "authors");
        authors.clear();
        authors.addAll(newAuthors);
    }

    private static String validTitle(String title) {
        return Guard.requireText(title, "title", MAX_TITLE_LENGTH);
    }

    private static Money validPrice(Money price) {
        Guard.requireNonNull(price, "price");
        Guard.require(price.isPositive(), "price", "must be greater than zero");
        return price;
    }

    private static Year validYear(Year year) {
        if (year == null) {
            return null;
        }
        return Guard.requireBetween(year, EARLIEST_YEAR, Year.of(9999), "publicationYear");
    }

    // ---------------------------------------------------------------- builder

    /**
     * Collects a book's attributes and validates them all at once in {@link #build()}.
     *
     * <p>Defaults: Turkish, active, empty stock, no ratings.
     */
    public static final class Builder {

        private BookId id;
        private int version;
        private Isbn isbn;
        private String title;
        private final List<Author> authors = new java.util.ArrayList<>();
        private Category category;
        private String publisher;
        private Year publicationYear;
        private Language language = Language.TURKISH;
        private int pageCount;
        private String description;
        private Money price;
        private Stock stock = Stock.EMPTY;
        private String coverImage;
        private boolean active = true;
        private RatingSummary ratings = RatingSummary.NONE;

        private Builder() {
        }

        /**
         * Marks the book as loaded from storage. Used by repositories only.
         *
         * @param storedId      the stored id
         * @param storedVersion the stored version
         * @return this builder
         */
        public Builder persisted(BookId storedId, int storedVersion) {
            this.id = storedId;
            this.version = storedVersion;
            return this;
        }

        /** @param value the ISBN @return this builder */
        public Builder isbn(Isbn value) {
            this.isbn = value;
            return this;
        }

        /** @param value the title @return this builder */
        public Builder title(String value) {
            this.title = value;
            return this;
        }

        /** @param value an author to add @return this builder */
        public Builder author(Author value) {
            this.authors.add(value);
            return this;
        }

        /** @param values authors to add @return this builder */
        public Builder authors(Collection<Author> values) {
            this.authors.addAll(values);
            return this;
        }

        /** @param value the category @return this builder */
        public Builder category(Category value) {
            this.category = value;
            return this;
        }

        /** @param value the publisher @return this builder */
        public Builder publisher(String value) {
            this.publisher = value;
            return this;
        }

        /** @param value the publication year @return this builder */
        public Builder publicationYear(Year value) {
            this.publicationYear = value;
            return this;
        }

        /** @param value the language @return this builder */
        public Builder language(Language value) {
            this.language = value;
            return this;
        }

        /** @param value the number of pages @return this builder */
        public Builder pageCount(int value) {
            this.pageCount = value;
            return this;
        }

        /** @param value the description @return this builder */
        public Builder description(String value) {
            this.description = value;
            return this;
        }

        /** @param value the price @return this builder */
        public Builder price(Money value) {
            this.price = value;
            return this;
        }

        /** @param value the stock @return this builder */
        public Builder stock(Stock value) {
            this.stock = value;
            return this;
        }

        /** @param value the cover image storage key @return this builder */
        public Builder coverImage(String value) {
            this.coverImage = value;
            return this;
        }

        /** @param value whether customers can see the book @return this builder */
        public Builder active(boolean value) {
            this.active = value;
            return this;
        }

        /** @param value the rating totals @return this builder */
        public Builder ratings(RatingSummary value) {
            this.ratings = value;
            return this;
        }

        /**
         * Validates the attributes and creates the book.
         *
         * @return the book
         * @throws com.bookstore.shared.kernel.error.ValidationException if an attribute is missing or invalid
         */
        public Book build() {
            return new Book(this);
        }
    }
}
