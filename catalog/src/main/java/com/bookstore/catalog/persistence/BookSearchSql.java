package com.bookstore.catalog.persistence;

import com.bookstore.catalog.domain.Book;
import com.bookstore.catalog.domain.BookSort;
import com.bookstore.catalog.domain.BookSpecifications.Active;
import com.bookstore.catalog.domain.BookSpecifications.BookSpecification;
import com.bookstore.catalog.domain.BookSpecifications.ByAuthor;
import com.bookstore.catalog.domain.BookSpecifications.InCategories;
import com.bookstore.catalog.domain.BookSpecifications.InLanguage;
import com.bookstore.catalog.domain.BookSpecifications.InStock;
import com.bookstore.catalog.domain.BookSpecifications.MatchesText;
import com.bookstore.catalog.domain.BookSpecifications.PricedFrom;
import com.bookstore.catalog.domain.BookSpecifications.PricedUpTo;
import com.bookstore.catalog.domain.BookSpecifications.StockBelow;
import com.bookstore.shared.jdbc.JdbcExecutor;
import com.bookstore.shared.kernel.paging.SortOrder;
import com.bookstore.shared.kernel.specification.AndSpecification;
import com.bookstore.shared.kernel.specification.MatchAll;
import com.bookstore.shared.kernel.specification.NotSpecification;
import com.bookstore.shared.kernel.specification.OrSpecification;
import com.bookstore.shared.kernel.specification.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Translates a {@code Specification<Book>} into the {@code WHERE} and {@code ORDER BY} parts of a
 * query on {@code books b}. Each rule in {@link com.bookstore.catalog.domain.BookSpecifications}
 * has one SQL counterpart here, and {@code and}, {@code or} and {@code not} keep their meaning, so
 * the database and the in-memory test repositories return the same books in the same order.
 *
 * <p>Only placeholders go into the SQL text; every value goes into {@link #params()}.
 */
final class BookSearchSql {

    private final List<Object> params = new ArrayList<>();
    private final String where;
    private final String orderBy;

    BookSearchSql(Specification<Book> specification, BookSort sort) {
        this.where = specification instanceof MatchAll<Book> ? "" : " WHERE " + condition(specification);
        this.orderBy = orderBy(sort);
    }

    /** @return {@code " WHERE ..."} or an empty string */
    String where() {
        return where;
    }

    /** @return {@code " ORDER BY ..."} */
    String orderBy() {
        return orderBy;
    }

    /** @return the values for the placeholders in {@link #where()}, in order */
    List<Object> params() {
        return List.copyOf(params);
    }

    private String condition(Specification<Book> specification) {
        return switch (specification) {
            case MatchAll<Book> all -> "1 = 1";
            case AndSpecification<Book> and -> "(" + condition(and.left()) + " AND " + condition(and.right()) + ")";
            case OrSpecification<Book> or -> "(" + condition(or.left()) + " OR " + condition(or.right()) + ")";
            case NotSpecification<Book> not -> "NOT (" + condition(not.negated()) + ")";
            case BookSpecification rule -> condition(rule);
            default -> throw new IllegalArgumentException(
                    "No SQL translation for " + specification.getClass().getName());
        };
    }

    private String condition(BookSpecification rule) {
        return switch (rule) {
            case Active active -> "b.active = TRUE";
            case InStock inStock -> "b.stock > 0";
            case StockBelow below -> {
                params.add(below.limit());
                yield "b.stock < ?";
            }
            case InCategories in -> {
                params.addAll(in.categoryIds());
                yield "b.category_id IN (" + JdbcExecutor.placeholders(in.categoryIds().size()) + ")";
            }
            case ByAuthor by -> {
                params.add(by.authorId());
                yield "EXISTS (SELECT 1 FROM book_authors ba WHERE ba.book_id = b.id AND ba.author_id = ?)";
            }
            case PricedFrom from -> {
                params.add(from.min());
                yield "b.price >= ?";
            }
            case PricedUpTo upTo -> {
                params.add(upTo.max());
                yield "b.price <= ?";
            }
            case InLanguage in -> {
                params.add(in.language().code());
                yield "b.language = ?";
            }
            case MatchesText text -> textCondition(text);
        };
    }

    private String textCondition(MatchesText text) {
        String pattern = "%" + escapeLike(text.searchKey()) + "%";
        StringBuilder condition = new StringBuilder("(b.title_key LIKE ? ESCAPE '!'"
                + " OR EXISTS (SELECT 1 FROM book_authors ba JOIN authors a ON a.id = ba.author_id"
                + " WHERE ba.book_id = b.id AND a.name_key LIKE ? ESCAPE '!')");
        params.add(pattern);
        params.add(pattern);
        text.isbnFragment().ifPresent(digits -> {
            condition.append(" OR b.isbn LIKE ?");
            params.add("%" + digits + "%");
        });
        return condition.append(")").toString();
    }

    /** Makes {@code %} and {@code _} in the search text match literally. */
    private static String escapeLike(String text) {
        return text.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }

    private static String orderBy(BookSort sort) {
        String direction = sort.order() == SortOrder.ASC ? "ASC" : "DESC";
        String field = switch (sort.field()) {
            case TITLE -> "b.title_key " + direction;
            case PRICE -> "b.price " + direction;
            // Books without a year come first in ascending order, as in the domain comparator.
            case PUBLICATION_YEAR -> "CASE WHEN b.publication_year IS NULL THEN 0 ELSE 1 END " + direction
                    + ", b.publication_year " + direction;
            case RATING -> "CASE WHEN b.rating_count = 0 THEN 0 ELSE b.rating_sum * 1.0 / b.rating_count END "
                    + direction;
        };
        return " ORDER BY " + field + ", b.title_key ASC, b.isbn ASC";
    }
}
