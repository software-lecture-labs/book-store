package com.bookstore.catalog.domain;

import com.bookstore.shared.kernel.money.Money;
import com.bookstore.shared.kernel.specification.Specification;
import com.bookstore.shared.kernel.text.TextNormalizer;
import com.bookstore.shared.kernel.validation.Guard;

import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The rules a catalog search is built from (Specification pattern). Each rule is a small record,
 * so the in-memory repository can test it against a book and the JDBC repository can translate
 * the same tree into SQL:
 *
 * <pre>{@code
 * Specification<Book> rule = BookSpecifications.active()
 *         .and(BookSpecifications.matchingText("sabahattin"))
 *         .and(BookSpecifications.pricedUpTo(Money.of("200")))
 *         .and(BookSpecifications.inStock());
 * }</pre>
 */
public final class BookSpecifications {

    /** Longest accepted search text. */
    public static final int MAX_TEXT_LENGTH = 200;

    /** At least four digits are needed before a query is also matched against ISBNs. */
    private static final Pattern ISBN_FRAGMENT = Pattern.compile("\\d{4,13}");

    private BookSpecifications() {
    }

    /** @return books that are still sold */
    public static Specification<Book> active() {
        return new Active();
    }

    /** @return books with at least one copy in stock */
    public static Specification<Book> inStock() {
        return new InStock();
    }

    /**
     * @param limit the number of copies
     * @return books with fewer copies in stock than the limit (ADM-8)
     */
    public static Specification<Book> stockBelow(int limit) {
        return new StockBelow(limit);
    }

    /**
     * @param categoryIds the categories, at least one
     * @return books in one of the categories
     */
    public static Specification<Book> inCategories(Set<CategoryId> categoryIds) {
        return new InCategories(categoryIds);
    }

    /**
     * @param authorId the author
     * @return books by the author
     */
    public static Specification<Book> byAuthor(AuthorId authorId) {
        return new ByAuthor(authorId);
    }

    /**
     * @param min the lowest price, inclusive
     * @return books that cost at least {@code min}
     */
    public static Specification<Book> pricedFrom(Money min) {
        return new PricedFrom(min);
    }

    /**
     * @param max the highest price, inclusive
     * @return books that cost at most {@code max}
     */
    public static Specification<Book> pricedUpTo(Money max) {
        return new PricedUpTo(max);
    }

    /**
     * @param language the language
     * @return books in the language
     */
    public static Specification<Book> inLanguage(Language language) {
        return new InLanguage(language);
    }

    /**
     * Returns books whose title or an author's name contains the words, ignoring case and Turkish
     * letters, or whose ISBN contains them when they are four or more digits.
     *
     * @param text the words; {@code null} or blank matches every book
     * @return the rule
     */
    public static Specification<Book> matchingText(String text) {
        String stripped = text == null ? "" : text.strip();
        return stripped.isEmpty() ? Specification.all() : new MatchesText(stripped);
    }

    /** The rules a book search can contain; the JDBC repository translates each of them. */
    public sealed interface BookSpecification extends Specification<Book>
            permits Active, InStock, StockBelow, InCategories, ByAuthor, PricedFrom, PricedUpTo, InLanguage, MatchesText {
    }

    /** Books that are still sold. */
    public record Active() implements BookSpecification {

        @Override
        public boolean isSatisfiedBy(Book book) {
            return book.isActive();
        }
    }

    /** Books with at least one copy in stock. */
    public record InStock() implements BookSpecification {

        @Override
        public boolean isSatisfiedBy(Book book) {
            return book.stock().isAvailable();
        }
    }

    /**
     * Books in one of some categories.
     *
     * @param categoryIds the categories, at least one
     */
    public record InCategories(Set<CategoryId> categoryIds) implements BookSpecification {

        /** Copies the ids and rejects an empty set. */
        public InCategories {
            categoryIds = Set.copyOf(Guard.requireNotEmpty(categoryIds, "categoryIds"));
        }

        @Override
        public boolean isSatisfiedBy(Book book) {
            return !book.category().isNew() && categoryIds.contains(book.category().id());
        }
    }

    /**
     * Books by an author.
     *
     * @param authorId the author
     */
    public record ByAuthor(AuthorId authorId) implements BookSpecification {

        /** Rejects a missing author. */
        public ByAuthor {
            Guard.requireNonNull(authorId, "authorId");
        }

        @Override
        public boolean isSatisfiedBy(Book book) {
            return book.authors().stream().anyMatch(author -> !author.isNew() && author.id().equals(authorId));
        }
    }

    /**
     * Books with fewer copies in stock than a limit.
     *
     * @param limit the number of copies, at least one
     */
    public record StockBelow(int limit) implements BookSpecification {

        /** Rejects a limit below one, which no book could meet. */
        public StockBelow {
            Guard.requirePositive(limit, "limit");
        }

        @Override
        public boolean isSatisfiedBy(Book book) {
            return book.stock().quantity() < limit;
        }
    }

    /**
     * Books that cost at least a price.
     *
     * @param min the lowest price, inclusive
     */
    public record PricedFrom(Money min) implements BookSpecification {

        /** Rejects a missing price. */
        public PricedFrom {
            Guard.requireNonNull(min, "minPrice");
        }

        @Override
        public boolean isSatisfiedBy(Book book) {
            return book.price().isAtLeast(min);
        }
    }

    /**
     * Books that cost at most a price.
     *
     * @param max the highest price, inclusive
     */
    public record PricedUpTo(Money max) implements BookSpecification {

        /** Rejects a missing price. */
        public PricedUpTo {
            Guard.requireNonNull(max, "maxPrice");
        }

        @Override
        public boolean isSatisfiedBy(Book book) {
            return max.isAtLeast(book.price());
        }
    }

    /**
     * Books in a language.
     *
     * @param language the language
     */
    public record InLanguage(Language language) implements BookSpecification {

        /** Rejects a missing language. */
        public InLanguage {
            Guard.requireNonNull(language, "language");
        }

        @Override
        public boolean isSatisfiedBy(Book book) {
            return book.language() == language;
        }
    }

    /**
     * Books whose title, an author's name or the ISBN contains some words.
     *
     * @param text the words, not blank, at most {@link #MAX_TEXT_LENGTH} characters
     */
    public record MatchesText(String text) implements BookSpecification {

        /** Rejects blank and over-long text. */
        public MatchesText {
            Guard.requireText(text, "text", MAX_TEXT_LENGTH);
        }

        /** @return the text as a search key: lower case, without Turkish letters */
        public String searchKey() {
            return TextNormalizer.searchKey(text);
        }

        /** @return the text's digits, if it looks like part of an ISBN */
        public Optional<String> isbnFragment() {
            String digits = text.replace("-", "").replace(" ", "");
            return ISBN_FRAGMENT.matcher(digits).matches() ? Optional.of(digits) : Optional.empty();
        }

        @Override
        public boolean isSatisfiedBy(Book book) {
            String key = searchKey();
            return TextNormalizer.searchKey(book.title()).contains(key)
                    || book.authors().stream().anyMatch(a -> TextNormalizer.searchKey(a.name()).contains(key))
                    || isbnFragment().map(digits -> book.isbn().value().contains(digits)).orElse(false);
        }
    }
}
