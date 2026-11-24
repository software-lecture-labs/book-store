package com.bookstore.review.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bookstore.catalog.domain.Book;
import com.bookstore.customer.domain.Customer;
import com.bookstore.shared.jdbc.DataAccessException;

import org.junit.jupiter.api.Test;

/**
 * The reviews table's own constraints, checked with raw SQL that bypasses the domain: H2 here,
 * PostgreSQL in {@link PostgresReviewSchemaConstraintsTest}.
 */
class ReviewSchemaConstraintsTest {

    private final ReviewDb db = newDb();
    private final Customer ayse = db.customers.customer("ayse@example.com");
    private final Book dune = db.catalog.book("Dune", "100", 10);

    /** @return the database the constraints are checked on */
    protected ReviewDb newDb() {
        return new ReviewDb();
    }

    @Test
    void ratingMustBeOneToFiveInTheDatabaseToo() {
        assertThatThrownBy(() -> db.jdbc.update("INSERT INTO reviews (book_id, user_id, rating, comment_text, "
                + "created_at, updated_at) VALUES (?, ?, 6, '', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                dune.id(), ayse.id()))
                .isInstanceOfSatisfying(DataAccessException.class, e -> assertThat(e.isConstraintViolation()).isTrue());
    }
}
