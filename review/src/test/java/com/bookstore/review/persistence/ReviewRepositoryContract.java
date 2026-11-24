package com.bookstore.review.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bookstore.catalog.domain.Book;
import com.bookstore.customer.domain.Customer;
import com.bookstore.customer.persistence.CustomerDb;
import com.bookstore.review.domain.Rating;
import com.bookstore.review.domain.Review;
import com.bookstore.review.domain.ReviewRepository;
import com.bookstore.shared.kernel.error.ConflictException;
import com.bookstore.shared.kernel.paging.PageRequest;
import com.bookstore.shared.kernel.testing.TestClock;

import java.time.Clock;
import java.time.Duration;

import org.junit.jupiter.api.Test;

/**
 * The contract of the review repository (ADR-0008, ADR-0023): the in-memory, JDBC and JPA
 * implementations, on H2 and on PostgreSQL, must all pass these tests.
 */
abstract class ReviewRepositoryContract {

    protected final ReviewDb db = newDb();

    /** @return a fresh, empty repository of the implementation under test */
    protected abstract ReviewDb newDb();

    private final ReviewRepository reviews = db.reviews;
    private final CustomerDb customers = db.customers;
    private final Customer ayse = customers.customer("ayse@example.com");
    private final Customer mehmet = customers.customer("mehmet@example.com");
    private final Book dune = db.catalog.book("Dune", "100", 10);

    @Test
    void reviewSurvivesTheRoundTripAndEdits() {
        Review review = reviews.save(Review.write(dune.id(), ayse.id(), Rating.of(4), "Güzel", TestClock.CLOCK));

        Review loaded = reviews.findByBookIdAndCustomerId(dune.id(), ayse.id()).orElseThrow();
        assertThat(loaded.id()).isEqualTo(review.id());
        assertThat(loaded.rating()).isEqualTo(Rating.of(4));
        assertThat(loaded.comment()).isEqualTo("Güzel");
        assertThat(loaded.createdAt()).isEqualTo(TestClock.NOW);

        loaded.edit(Rating.of(5), null, Clock.offset(TestClock.CLOCK, Duration.ofDays(1)));
        reviews.save(loaded);
        Review edited = reviews.findById(review.id()).orElseThrow();
        assertThat(edited.rating()).isEqualTo(Rating.of(5));
        assertThat(edited.comment()).isEmpty();
        assertThat(edited.updatedAt()).isAfter(edited.createdAt());
    }

    @Test
    void oneReviewPerCustomerAndBook() {
        reviews.save(Review.write(dune.id(), ayse.id(), Rating.of(4), null, TestClock.CLOCK));

        assertThatThrownBy(() -> reviews.save(Review.write(dune.id(), ayse.id(), Rating.of(1), null, TestClock.CLOCK)))
                .isInstanceOfSatisfying(ConflictException.class,
                        e -> assertThat(e.code()).isEqualTo("review.duplicate"));
    }

    @Test
    void anOutdatedCopyCannotOverwriteANewerOne() {
        Review stored = reviews.save(Review.write(dune.id(), ayse.id(), Rating.of(4), "Güzel", TestClock.CLOCK));
        Review first = reviews.findById(stored.id()).orElseThrow();
        Review second = reviews.findById(stored.id()).orElseThrow();
        Clock later = Clock.offset(TestClock.CLOCK, Duration.ofDays(1));

        first.edit(Rating.of(2), "İlk", later);
        reviews.save(first);
        second.edit(Rating.of(5), "İkinci", later);

        assertThatThrownBy(() -> reviews.save(second))
                .isInstanceOfSatisfying(ConflictException.class,
                        e -> assertThat(e.code()).isEqualTo(ConflictException.CONCURRENT_MODIFICATION));
        assertThat(reviews.findById(stored.id()).orElseThrow().comment()).isEqualTo("İlk");
        assertThat(first.version()).isGreaterThan(second.version());
    }

    @Test
    void listsRatingsAndDeletes() {
        Review first = reviews.save(Review.write(dune.id(), ayse.id(), Rating.of(5), null, TestClock.CLOCK));
        reviews.save(Review.write(dune.id(), mehmet.id(), Rating.of(2), null,
                Clock.offset(TestClock.CLOCK, Duration.ofMinutes(1))));

        assertThat(reviews.findByBookId(dune.id(), PageRequest.first()).content())
                .extracting(Review::customerId).containsExactly(mehmet.id(), ayse.id());
        assertThat(reviews.findRatingsByBookId(dune.id())).containsExactlyInAnyOrder(Rating.of(5), Rating.of(2));
        assertThat(reviews.findAll(PageRequest.first()).totalElements()).isEqualTo(2);

        reviews.delete(first);
        assertThat(reviews.findRatingsByBookId(dune.id())).containsExactly(Rating.of(2));
    }
}
